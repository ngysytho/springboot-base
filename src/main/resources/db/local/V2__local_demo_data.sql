-- Local profile ONLY. Keycloak subjects match the imported realm's explicit user IDs.
INSERT INTO organizations(id,name,created_at,updated_at) VALUES
('11111111-1111-1111-1111-111111111111','Demo Learning Centre',now(),now()),
('22222222-2222-2222-2222-222222222222','Other Learning Centre',now(),now());
INSERT INTO user_profiles(id,subject,display_name,created_at,updated_at) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','Demo Teacher',now(),now()),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb','bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb','Demo Student',now(),now());
INSERT INTO organization_memberships(id,organization_id,user_profile_id,role,created_at,updated_at) VALUES
('cccccccc-cccc-cccc-cccc-cccccccccccc','11111111-1111-1111-1111-111111111111','aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','TEACHER',now(),now()),
('dddddddd-dddd-dddd-dddd-dddddddddddd','11111111-1111-1111-1111-111111111111','bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb','STUDENT',now(),now());
