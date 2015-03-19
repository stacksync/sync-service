------------------------------
-- Get by id
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION get_device_by_id(uid uuid, did uuid)
RETURNS TABLE(id uuid, latest_revision text, owner_id uuid, is_shared boolean, is_encrypted boolean, swift_container text, swift_url text, created_at timestamp) AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text);
	SELECT * FROM device WHERE id = did;
$$ LANGUAGE plproxy;

------------------------------
-- Add device
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION add_device(uid uuid, name text, os text, last_ip inet, app_version text)
RETURNS uuid AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part
CREATE OR REPLACE FUNCTION add_device(uid uuid, name text, os text, last_ip inet, app_version text)
RETURNS uuid AS $$
DECLARE
	did uuid;
BEGIN
	did := (select uuid_generate_v1());	
	INSERT INTO device (id, name, user_id, os, created_at, last_access_at, last_ip, app_version) VALUES (did, $2, $1, $3, now(), now(), $4, $5);
	return did;
END;
$$ LANGUAGE plpgsql;

------------------------------
-- Update device
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION update_device(uid uuid, did uuid, last_ip inet, app_version text)
RETURNS integer AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION update_device(uid uuid, did uuid, last_ip inet, app_version text)
RETURNS integer AS $$
	UPDATE device SET last_access_at = now(), last_ip = $3, app_version = $4 WHERE id = $2 and user_id = $1;
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Delete device
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION delete_device(uid uuid, did uuid)
RETURNS integer AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION delete_device(uid uuid, did uuid)
RETURNS integer AS $$
	DELETE FROM device WHERE id = $2;
	SELECT 1;
$$ LANGUAGE SQL;


