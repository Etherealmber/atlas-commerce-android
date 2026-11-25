import unittest

from worker_route_audit import composed_worker_routes, worker_has_route


class WorkerRouteAuditTest(unittest.TestCase):
    def test_resolves_constant_prefix_for_registered_hono_routes(self):
        worker = """
        export const NATIVE_V2 = '/api/auth/desktop/v2';
        app.post(`${NATIVE_V2}/phone/prepare`, async c => c.json({ success: true }));
        app.post(`${NATIVE_V2}/phone/decision`, async c => c.json({ success: true }));
        """

        routes = composed_worker_routes(worker)
        self.assertIn("/api/auth/desktop/v2/phone/prepare", routes)
        self.assertIn("/api/auth/desktop/v2/phone/decision", routes)
        self.assertTrue(worker_has_route("/api/auth/desktop/v2/phone/prepare", worker))
        self.assertTrue(worker_has_route("/api/auth/desktop/v2/phone/decision", worker))

    def test_does_not_count_constants_or_non_route_template_strings_as_endpoints(self):
        worker = """
        export const NATIVE_V2 = '/api/auth/desktop/v2';
        const advertisedOnly = `${NATIVE_V2}/phone/prepare`;
        export const V2_PHONE_POSTS = new Set(['phone/prepare'].map(x => `${NATIVE_V2}/${x}`));
        """

        self.assertEqual(set(), composed_worker_routes(worker))
        self.assertFalse(worker_has_route("/api/auth/desktop/v2/phone/prepare", worker))

    def test_keeps_direct_literal_prefix_matching(self):
        worker = "app.post('/api/inventory/offline-operations', handler);"
        self.assertTrue(worker_has_route("/api/inventory/offline-operations", worker))


if __name__ == "__main__":
    unittest.main()
