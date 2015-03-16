------------------------------
-- Insert user
------------------------------

-- Proxy # uuid_generate_v1()

CREATE OR REPLACE FUNCTION add_user(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer)
RETURNS SETOF integer AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(id::text) ;
$$ LANGUAGE plproxy;

-- Part
CREATE OR REPLACE FUNCTION add_user(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer)
RETURNS integer AS $$
	INSERT INTO user1 (id, email, name, swift_user, swift_account, quota_limit, quota_used) VALUES ($1, $2, $3, $4, $5, $6, $7);
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Find by Id
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION find_user_by_id(uid uuid)
RETURNS TABLE(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer, created_at timestamp) AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text) ;
	SELECT id, name, email, swift_user, swift_account, quota_limit, quota_used, created_at FROM user1 WHERE id=$1;

$$ LANGUAGE plproxy;

------------------------------
-- Get user by email
------------------------------

CREATE OR REPLACE FUNCTION get_user_by_email(mail text)
RETURNS TABLE(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer, created_at timestamp) AS $$
    CLUSTER 'usercluster';
    RUN ON ALL;
	SELECT * FROM user1 WHERE email=$1;

$$ LANGUAGE plproxy;

------------------------------
-- Find all
------------------------------

CREATE OR REPLACE FUNCTION find_all_users()
RETURNS TABLE(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer, created_at timestamp) AS $$
    CLUSTER 'usercluster';
    RUN ON ALL;
	SELECT * FROM user1;

$$ LANGUAGE plproxy;

------------------------------
-- Update
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION update_user(uid uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer)
RETURNS integer AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION update_user(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer)
RETURNS integer AS $$
	UPDATE user1 SET email = $2, name = $3, swift_user = $4, swift_account = $5, quota_limit = $6, quota_used = $7 WHERE id = $1;
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Delete
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION delete_user(uid uuid)
RETURNS integer AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION delete_user(uid uuid)
RETURNS integer AS $$
	DELETE FROM user1 WHERE id = $1;
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Find user by itemid
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION find_user_by_item_id(id bigint)
RETURNS TABLE(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer, created_at timestamp) AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(id) ;
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION find_user_by_item_id(id bigint)
RETURNS TABLE(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer, created_at timestamp) AS $$
	SELECT u.* FROM item i INNER JOIN workspace_user wu ON i.workspace_id = wu.workspace_id INNER JOIN user1 u ON wu.user_id = u.id WHERE i.id = $1;
$$ LANGUAGE SQL;


