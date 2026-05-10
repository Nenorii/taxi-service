import os
import sys
import time
import uuid
import unittest
from concurrent.futures import ThreadPoolExecutor, as_completed

import requests
from requests import RequestException


USER_BASE_URL = os.getenv("USER_BASE_URL", "http://localhost:8081")
TRIP_BASE_URL = os.getenv("TRIP_BASE_URL", "http://localhost:8082")
NOTIFICATION_BASE_URL = os.getenv("NOTIFICATION_BASE_URL", "http://localhost:8083")
RABBIT_API_URL = os.getenv("RABBIT_API_URL", "http://localhost:15672/api")
RABBIT_USER = os.getenv("RABBIT_USER", "guest")
RABBIT_PASSWORD = os.getenv("RABBIT_PASSWORD", "guest")

LOG_FILE = "test_execution_log.txt"


class DetailedLogger:
    def __init__(self, log_file):
        self.log_file = log_file
        
    def log(self, message):
        with open(self.log_file, 'a', encoding='utf-8') as f:
            f.write(message + '\n')
    
    def log_step(self, step_num, title):
        self.log(f"\n{'#'*80}")
        self.log(f"# STEP {step_num:02d}: {title}")
        self.log(f"{'#'*80}")
    
    def log_request(self, method, url, headers=None, params=None, json_data=None):
        self.log(f"  >>> REQUEST: {method} {url}")
        if headers:
            safe_headers = dict(headers)
            if "Authorization" in safe_headers:
                safe_headers["Authorization"] = "Bearer ***"
            self.log(f"      headers: {self._pretty(safe_headers)}")
        if params:
            self.log(f"      params:  {self._pretty(params)}")
        if json_data:
            self.log(f"      json:    {self._pretty(json_data)}")
    
    def log_response(self, status_code, body):
        self.log(f"  <<< RESPONSE: {status_code}")
        if body:
            try:
                self.log(f"      body: {self._pretty(body)}")
            except:
                self.log(f"      body: {body}")
    
    def _pretty(self, value):
        try:
            return requests.models.complexjson.dumps(value, ensure_ascii=False, indent=2)
        except Exception:
            return str(value)


detailed_logger = DetailedLogger(LOG_FILE)


class TaxiServiceSystemTest(unittest.TestCase):
    passenger_identifier = None
    driver_identifier = None
    trip_identifier = None
    auth_token = None
    step_index = 0

    @classmethod
    def setUpClass(cls):
        if os.path.exists(LOG_FILE):
            os.remove(LOG_FILE)

    def log_step(self, title):
        TaxiServiceSystemTest.step_index += 1
        detailed_logger.log_step(self.step_index, title)

    def pretty(self, value):
        return detailed_logger._pretty(value)

    def require_trip(self):
        if self.trip_identifier is None:
            self.skipTest("Trip was not created in test_04_create_new_trip")

    def request_json(self, method, url, expected_status, **kwargs):
        request_json_payload = kwargs.get("json")
        request_params = kwargs.get("params")
        request_headers = kwargs.get("headers") or {}
        
        detailed_logger.log_request(method, url, request_headers, request_params, request_json_payload)

        response = requests.request(method, url, timeout=15, **kwargs)
        
        response_json = None
        if response.text.strip():
            try:
                response_json = response.json()
            except Exception:
                pass
        
        detailed_logger.log_response(response.status_code, response_json)

        self.assertEqual(
            expected_status,
            response.status_code,
            msg=f"{method} {url} -> {response.status_code}, body: {response.text}",
        )
        
        return response_json

    def create_trip_with_retry(self, headers, payload, attempts=3, delay_seconds=1.5):
        last_error = None
        for i in range(attempts):
            try:
                return self.request_json(
                    "POST",
                    f"{TRIP_BASE_URL}/trips",
                    201,
                    headers=headers,
                    json=payload,
                )
            except AssertionError as exc:
                last_error = exc
                if i < attempts - 1:
                    time.sleep(delay_seconds)
                    continue
                raise
            except RequestException as exc:
                last_error = exc
                if i < attempts - 1:
                    time.sleep(delay_seconds)
                    continue
                raise
        raise AssertionError(f"Failed to create trip after retries: {last_error}")

    def get_rabbit_trip_events_publish_count(self):
        try:
            response = requests.get(
                f"{RABBIT_API_URL}/queues/%2F/trip.events.queue",
                auth=(RABBIT_USER, RABBIT_PASSWORD),
                timeout=5,
            )
            if response.status_code != 200:
                return None
            data = response.json()
            stats = data.get("message_stats") or {}
            return int(stats.get("publish", 0))
        except Exception:
            return None

    def test_01_create_passenger_account(self):
        self.log_step("Create passenger account")
        unique_id = uuid.uuid4().hex[:8]
        payload = {
            "email": f"rider_{unique_id}@example.org",
            "password": "qwerty789",
            "name": f"[SYSTEMTEST] Passenger Account #{unique_id}",
            "phone": f"+7495{int(time.time()) % 10_000_000:07d}",
            "userType": "PASSENGER",
        }
        data = self.request_json(
            "POST",
            f"{USER_BASE_URL}/auth/register",
            201,
            json=payload,
        )
        self.assertIn("token", data)
        self.assertIn("userId", data)
        TaxiServiceSystemTest.auth_token = data["token"]
        TaxiServiceSystemTest.passenger_identifier = data["userId"]

    def test_02_create_driver_account(self):
        self.log_step("Create driver account and mark as available")
        unique_id = uuid.uuid4().hex[:8]
        payload = {
            "email": f"chauffeur_{unique_id}@example.org",
            "password": "qwerty789",
            "name": f"[SYSTEMTEST] Driver Account #{unique_id}",
            "phone": f"+7496{int(time.time()) % 10_000_000:07d}",
            "userType": "DRIVER",
            "licenseNumber": f"DRV-{unique_id.upper()}",
        }
        data = self.request_json(
            "POST",
            f"{USER_BASE_URL}/auth/register",
            201,
            json=payload,
        )
        self.assertIn("userId", data)
        TaxiServiceSystemTest.driver_identifier = data["userId"]

        self.assertIsNotNone(self.auth_token, "Passenger token must be created first")
        headers = {"Authorization": f"Bearer {self.auth_token}"}
        updated = self.request_json(
            "PATCH",
            f"{USER_BASE_URL}/drivers/{self.driver_identifier}/status",
            200,
            headers=headers,
            json={"status": "AVAILABLE"},
        )
        self.assertEqual("AVAILABLE", updated["status"])

    def test_03_verify_account_details(self):
        self.log_step("Retrieve and verify passenger and driver profiles")
        self.assertIsNotNone(self.passenger_identifier, "Passenger must be created first")
        self.assertIsNotNone(self.driver_identifier, "Driver must be created first")
        self.assertIsNotNone(self.auth_token, "Token must be received first")
        headers = {"Authorization": f"Bearer {self.auth_token}"}

        passenger_profile = self.request_json(
            "GET",
            f"{USER_BASE_URL}/passengers/{self.passenger_identifier}",
            200,
            headers=headers,
        )
        self.assertEqual(self.passenger_identifier, passenger_profile["id"])

        driver_profile = self.request_json(
            "GET",
            f"{USER_BASE_URL}/drivers/{self.driver_identifier}",
            200,
            headers=headers,
        )
        self.assertEqual(self.driver_identifier, driver_profile["id"])

    def test_04_create_new_trip(self):
        self.log_step("Create a new trip request")
        self.assertIsNotNone(self.passenger_identifier, "Passenger must be created first")
        self.assertIsNotNone(self.auth_token, "Token must be received first")

        headers = {"Authorization": f"Bearer {self.auth_token}"}
        payload = {
            "passengerId": self.passenger_identifier,
            "origin": "Saint Petersburg, Nevsky Ave 1",
            "destination": "Saint Petersburg, Palace Square",
        }
        trip = self.create_trip_with_retry(headers, payload)
        self.assertIn("id", trip)
        self.assertIsNotNone(trip.get("driverId"))
        self.assertEqual("DRIVER_ASSIGNED", trip["status"])
        
        # Проверка наличия поля price (без проверки значения)
        self.assertIn("price", trip, "Price field missing in trip response")
        
        # Логируем цену для отладки
        detailed_logger.log(f"     Trip price received: {trip.get('price')}")
        
        TaxiServiceSystemTest.trip_identifier = trip["id"]

    def test_05_fetch_trip_and_history(self):
        self.log_step("Fetch trip details and passenger trip history")
        self.require_trip()
        headers = {"Authorization": f"Bearer {self.auth_token}"}

        trip_details = self.request_json(
            "GET",
            f"{TRIP_BASE_URL}/trips/{self.trip_identifier}",
            200,
            headers=headers,
        )
        self.assertEqual(self.trip_identifier, trip_details["id"])

        trip_list = self.request_json(
            "GET",
            f"{TRIP_BASE_URL}/trips",
            200,
            headers=headers,
            params={"passenger_id": self.passenger_identifier},
        )
        self.assertTrue(any(t["id"] == self.trip_identifier for t in trip_list))

    def test_06_advance_trip_state(self):
        self.log_step("Advance trip status through IN_PROGRESS to COMPLETED")
        self.require_trip()
        headers = {"Authorization": f"Bearer {self.auth_token}"}

        ongoing = self.request_json(
            "PATCH",
            f"{TRIP_BASE_URL}/trips/{self.trip_identifier}/status",
            200,
            headers=headers,
            json={"status": "IN_PROGRESS"},
        )
        self.assertEqual("IN_PROGRESS", ongoing["status"])

        finished = self.request_json(
            "PATCH",
            f"{TRIP_BASE_URL}/trips/{self.trip_identifier}/status",
            200,
            headers=headers,
            json={"status": "COMPLETED"},
        )
        self.assertEqual("COMPLETED", finished["status"])

    def test_07_submit_trip_rating(self):
        self.log_step("Submit rating for completed trip")
        self.require_trip()
        headers = {"Authorization": f"Bearer {self.auth_token}"}

        rated_trip = self.request_json(
            "POST",
            f"{TRIP_BASE_URL}/trips/{self.trip_identifier}/rate",
            200,
            headers=headers,
            json={"rating": 5},
        )
        self.assertEqual(5, rated_trip["rating"])

    def test_08_retrieve_statistics(self):
        self.log_step("Retrieve trip statistics")
        headers = {"Authorization": f"Bearer {self.auth_token}"}
        statistics = self.request_json(
            "GET",
            f"{TRIP_BASE_URL}/trips/stats",
            200,
            headers=headers,
        )
        self.assertGreaterEqual(statistics.get("tripsToday", 0), 1)
        self.assertGreaterEqual(statistics.get("averagePrice", 0), 0)

    def test_09_verify_notifications_generated(self):
        self.log_step("Verify notifications generated from trip events")
        self.require_trip()

        notification_list = []
        for _ in range(10):
            notification_list = self.request_json(
                "GET",
                f"{NOTIFICATION_BASE_URL}/notifications",
                200,
                params={"trip_id": self.trip_identifier},
            )
            if notification_list:
                break
            time.sleep(1)

        self.assertGreaterEqual(
            len(notification_list),
            1,
            msg="No notifications found for trip, check RabbitMQ/worker service",
        )
        notification_statuses = {n["status"] for n in notification_list}
        self.assertTrue(
            notification_statuses & {"PENDING", "IN_PROGRESS", "SENT", "FAILED"},
            msg=f"Unexpected notification statuses: {notification_statuses}",
        )

    def test_10_create_custom_notification(self):
        self.log_step("Create custom notification via API endpoint")
        self.require_trip()
        payload = {
            "tripId": self.trip_identifier,
            "recipientType": "PASSENGER",
            "recipientId": self.passenger_identifier,
            "message": f"[SYSTEMTEST] Custom notification generated at {int(time.time())}",
        }
        created_notification = self.request_json(
            "POST",
            f"{NOTIFICATION_BASE_URL}/notifications",
            201,
            json=payload,
        )
        self.assertEqual(self.trip_identifier, created_notification["tripId"])

    def test_11_ensure_driver_assignment_uniqueness(self):
        self.log_step("Verify driver assigned to only one concurrent trip")
        concurrent_passengers = []
        for _ in range(4):
            unique_suffix = uuid.uuid4().hex[:8]
            registration = self.request_json(
                "POST",
                f"{USER_BASE_URL}/auth/register",
                201,
                json={
                    "email": f"concurrent_{unique_suffix}@test.net",
                    "password": "secret123",
                    "name": f"[SYSTEMTEST] Concurrent Passenger {unique_suffix}",
                    "phone": f"+7977{int(time.time() * 1000) % 10_000_000:07d}",
                    "userType": "PASSENGER",
                },
            )
            concurrent_passengers.append((registration["userId"], registration["token"]))

        for _ in range(4):
            unique_suffix = uuid.uuid4().hex[:8]
            registration = self.request_json(
                "POST",
                f"{USER_BASE_URL}/auth/register",
                201,
                json={
                    "email": f"concurrent_driver_{unique_suffix}@test.net",
                    "password": "secret123",
                    "name": f"[SYSTEMTEST] Concurrent Driver {unique_suffix}",
                    "phone": f"+7988{int(time.time() * 1000) % 10_000_000:07d}",
                    "userType": "DRIVER",
                    "licenseNumber": f"CONC-{unique_suffix.upper()}",
                },
            )
            driver_id = registration["userId"]
            self.request_json(
                "PATCH",
                f"{USER_BASE_URL}/drivers/{driver_id}/status",
                200,
                headers={"Authorization": f"Bearer {concurrent_passengers[0][1]}"},
                json={"status": "AVAILABLE"},
            )

        def create_trip_request(user_data):
            passenger_id, access_token = user_data
            request_headers = {"Authorization": f"Bearer {access_token}"}
            trip_payload = {
                "passengerId": passenger_id,
                "origin": "Moscow, Tverskaya Street",
                "destination": "Moscow, Red Square",
            }
            http_response = requests.post(
                f"{TRIP_BASE_URL}/trips", headers=request_headers, json=trip_payload, timeout=15
            )
            return http_response.status_code, http_response.text

        assigned_drivers = []
        with ThreadPoolExecutor(max_workers=4) as executor:
            future_tasks = [executor.submit(create_trip_request, user) for user in concurrent_passengers]
            for completed_future in as_completed(future_tasks):
                status_code, response_body = completed_future.result()
                if status_code == 201:
                    response_data = requests.models.complexjson.loads(response_body)
                    assigned_drivers.append(response_data.get("driverId"))

        self.assertGreaterEqual(
            len(assigned_drivers),
            2,
            msg="Expected at least 2 successful concurrent assignments",
        )
        self.assertEqual(
            len(assigned_drivers),
            len(set(assigned_drivers)),
            msg=f"Same driver assigned concurrently: {assigned_drivers}",
        )

    def test_12_validate_rabbitmq_event_emission(self):
        self.log_step("Validate RabbitMQ queue receives trip events")
        pre_publish_count = self.get_rabbit_trip_events_publish_count()
        if pre_publish_count is None:
            self.skipTest("RabbitMQ management API is unavailable on localhost:15672")

        unique_suffix = uuid.uuid4().hex[:8]
        passenger_registration = self.request_json(
            "POST",
            f"{USER_BASE_URL}/auth/register",
            201,
            json={
                "email": f"rabbit_passenger_{unique_suffix}@test.net",
                "password": "secret123",
                "name": f"[SYSTEMTEST] RabbitMQ Passenger",
                "phone": f"+7999{int(time.time() * 1000) % 10_000_000:07d}",
                "userType": "PASSENGER",
            },
        )
        driver_registration = self.request_json(
            "POST",
            f"{USER_BASE_URL}/auth/register",
            201,
            json={
                "email": f"rabbit_driver_{unique_suffix}@test.net",
                "password": "secret123",
                "name": f"[SYSTEMTEST] RabbitMQ Driver",
                "phone": f"+7900{int(time.time() * 1000) % 10_000_000:07d}",
                "userType": "DRIVER",
                "licenseNumber": f"RABBIT-{unique_suffix.upper()}",
            },
        )

        access_token = passenger_registration["token"]
        self.request_json(
            "PATCH",
            f"{USER_BASE_URL}/drivers/{driver_registration['userId']}/status",
            200,
            headers={"Authorization": f"Bearer {access_token}"},
            json={"status": "AVAILABLE"},
        )

        self.request_json(
            "POST",
            f"{TRIP_BASE_URL}/trips",
            201,
            headers={"Authorization": f"Bearer {access_token}"},
            json={
                "passengerId": passenger_registration["userId"],
                "origin": "Moscow, Garden Ring",
                "destination": "Moscow, Third Ring Road",
            },
        )

        post_publish_count = pre_publish_count
        for _ in range(5):
            time.sleep(1)
            current_count = self.get_rabbit_trip_events_publish_count()
            if current_count is not None:
                post_publish_count = current_count
            if post_publish_count > pre_publish_count:
                break

        self.assertGreater(
            post_publish_count,
            pre_publish_count,
            msg=f"Queue publish counter did not increase: before={pre_publish_count}, after={post_publish_count}",
        )


if __name__ == "__main__":
    if os.path.exists(LOG_FILE):
        os.remove(LOG_FILE)
    
    test_suite = unittest.TestLoader().loadTestsFromTestCase(TaxiServiceSystemTest)
    test_runner = unittest.TextTestRunner(verbosity=2)
    test_result = test_runner.run(test_suite)
    
    print(f"\nTest execution log saved to: {LOG_FILE}")
    
    sys.exit(0 if test_result.wasSuccessful() else 1)