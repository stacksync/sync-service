------------------------------
-- Get by id
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION get_workspace_by_id(uid uuid, wid uuid)
RETURNS TABLE(id uuid, latest_revision text, owner_id uuid, is_shared boolean, is_encrypted boolean, swift_container text, swift_url text, user_id uuid, workspace_name text, parent_item_id bigint) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
    SELECT w.id, w.latest_revision, w.owner_id, w.is_shared, w.is_encrypted, w.swift_container, w.swift_url, wu.user_id, wu.workspace_name, wu.parent_item_id FROM workspace w INNER JOIN workspace_user wu ON wu.workspace_id = w.id WHERE w.id = wid;
$$ LANGUAGE plproxy;

------------------------------
-- Get by user id
------------------------------

CREATE OR REPLACE FUNCTION get_workspace_by_userid(uid uuid)
RETURNS TABLE(id uuid, latest_revision text, owner_id uuid, is_shared boolean, is_encrypted boolean, swift_container text, swift_url text, user_id uuid, workspace_name text, parent_item_id bigint) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
    SELECT w.id, w.latest_revision, w.owner_id, w.is_shared, w.is_encrypted, w.swift_container, w.swift_url, wu.user_id, wu.workspace_name, wu.parent_item_id FROM workspace w INNER JOIN workspace_user wu ON wu.workspace_id = w.id WHERE wu.user_id=uid;
$$ LANGUAGE plproxy;


------------------------------
-- Get default workspace by user id
------------------------------

CREATE OR REPLACE FUNCTION get_default_workspace_by_userid(uid uuid)
RETURNS TABLE(id uuid, latest_revision text, owner_id uuid, is_shared boolean, is_encrypted boolean, swift_container text, swift_url text, user_id uuid, workspace_name text, parent_item_id bigint) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
    SELECT w.id, w.latest_revision, w.owner_id, w.is_shared, w.is_encrypted, w.swift_container, w.swift_url, wu.user_id, wu.workspace_name, wu.parent_item_id FROM workspace w INNER JOIN workspace_user wu ON wu.workspace_id = w.id WHERE w.owner_id=uid AND w.is_shared = false LIMIT 1;

$$ LANGUAGE plproxy;

------------------------------
-- Add workspace
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION add_workspace(uid uuid, latest_revision text, owner_id uuid, is_shared boolean, is_encrypted boolean, swift_container text, swift_url text)
RETURNS uuid AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part
CREATE OR REPLACE FUNCTION add_workspace(uid uuid, latest_revision text, owner_id uuid, is_shared boolean, is_encrypted boolean, swift_container text, swift_url text)
RETURNS uuid AS $$
DECLARE
    wid uuid;
BEGIN
    wid := (select uuid_generate_v1());   	 
    INSERT INTO workspace (id, latest_revision, owner_id, is_shared, is_encrypted, swift_container, swift_url) VALUES (wid, $2, $3, $4, $5, $6, $7);
    INSERT INTO workspace_user (workspace_id, user_id, workspace_name, parent_item_id) VALUES (wid, uid, 'default', NULL);
    return wid;
END;
$$ LANGUAGE plpgsql;

------------------------------
-- Update workspace
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION update_workspace(uid uuid, wid uuid, workspace_name text, parent_item_id bigint)
RETURNS integer AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION update_workspace(uid uuid, wid uuid, workspace_name text, parent_item_id bigint)
RETURNS integer AS $$
    UPDATE workspace_user SET workspace_name = $3, parent_item_id = $4, modified_at = now() WHERE user_id = $1 AND workspace_id = $2;
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Delete workspace
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION delete_workspace(uid uuid, wid uuid)
RETURNS integer AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION delete_workspace(uid uuid, wid uuid)
RETURNS integer AS $$
    DELETE FROM workspace WHERE id = $2;
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Add user into a workspace
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION add_user_workspace(uid uuid, wid uuid, workspace_name text, parent_item_id bigint)
RETURNS integer AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION add_user_workspace(uid uuid, wid uuid, workspace_name text, parent_item_id bigint)
RETURNS integer AS $$
    INSERT INTO workspace_user (workspace_id, user_id, workspace_name, parent_item_id) VALUES ($2, $1, $3, $4);
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Delete user into a workspace
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION delete_user_workspace(uid uuid, wid uuid)
RETURNS integer AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

-- Part

CREATE OR REPLACE FUNCTION delete_user_workspace(uid uuid, wid uuid)
RETURNS integer AS $$
    DELETE FROM workspace_user WHERE user_id=$1 AND workspace_id=$2;
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Get workspace by item id
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION get_workspace_by_item_id(item_id bigint)
RETURNS TABLE(id uuid, latest_revision text, owner_id uuid, is_shared boolean, is_encrypted boolean, swift_container text, swift_url text, created_at timestamp) AS $$
	CLUSTER 'usercluster';
	RUN ON ALL;
    SELECT * FROM workspace w INNER JOIN workspace_user wu ON wu.workspace_id = w.id INNER JOIN item i ON w.id = i.workspace_id WHERE i.id = $1;
$$ LANGUAGE plproxy;


------------------------------
-- Get workspace_user relation
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION get_workspace_by_item_id(wid uuid)
RETURNS TABLE(workspace_id uuid,  user_id uuid, workspace_name text, parent_item_id bigint, created_at timestamp, modified_at timestamp) AS $$
	CLUSTER 'usercluster';
	RUN ON ALL;
    SELECT u.*, CASE WHEN u.id=w.owner_id THEN True ELSE False END AS is_owner, wu.created_at AS joined_at, wu.workspace_id FROM workspace w INNER JOIN workspace_user wu ON wu.workspace_id = w.id INNER JOIN user1 u ON wu.user_id = u.id WHERE w.id = $1;
$$ LANGUAGE plproxy;

