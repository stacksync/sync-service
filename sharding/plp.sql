------------------------------
-- Get by id
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION get_device_by_id(uid uuid, did uuid)
RETURNS TABLE(id uuid, name text, user_id uuid, os text, created_at timestamp, last_access_at timestamp, last_ip inet, app_version text) AS $$
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

------------------------------
-- Add device uuid
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION add_device(uid uuid, did uuid, name text, os text, last_ip inet, app_version text)
RETURNS uuid AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;


------------------------------
-- Update device
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION update_device(uid uuid, did uuid, last_ip inet, app_version text)
RETURNS integer AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

------------------------------
-- Delete device
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION delete_device(uid uuid, did uuid)
RETURNS integer AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

------------------------------
-- Add item version
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION add_item_version(uid uuid, item_id bigint, device_id uuid, version bigint, checksum bigint, status text, size bigint, modified_at timestamp)
RETURNS SETOF bigint AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

------------------------------
-- Find by item id and version
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION find_by_item_id_and_version(uid uuid, id bigint, version bigint)
RETURNS TABLE(item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, is_folder boolean, mimetype text, workspace_id uuid, version bigint, device_id uuid, checksum bigint, status text, size bigint, modified_at timestamp, chunks text[]) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
    SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, i.is_folder, i.mimetype, i.workspace_id, iv.version, iv.device_id, iv.checksum, iv.status, iv.size, iv.modified_at, get_chunks(iv.id) AS chunks FROM item_version iv INNER JOIN item i ON i.id = iv.item_id WHERE iv.item_id = $2 and iv.version = $3;
$$ LANGUAGE plproxy;

------------------------------
-- Insert chunk
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION insert_chunk(uid uuid, item_version_id bigint,  client_chunk_name text, chunk_order integer)
RETURNS integer AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

------------------------------
-- Insert chunks // this function is built dynamically
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION dynamic_query(uid uuid, sql text)
RETURNS void AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
$$ LANGUAGE plproxy;

-- Proxy

CREATE OR REPLACE FUNCTION dynamic_void_query(uid uuid, sql text)
RETURNS void AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
$$ LANGUAGE plproxy;

------------------------------
-- Find chunks
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION find_chunks(uid uuid, item_version_id bigint)
RETURNS TABLE(item_version_id bigint, client_chunk_name text, chunk_order integer) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
    SELECT ivc.* FROM item_version_chunk ivc WHERE ivc.item_version_id=$2 ORDER BY ivc.chunk_order ASC;
$$ LANGUAGE plproxy;

------------------------------
-- Get by id
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION get_item_by_id(uid uuid, iid bigint)
RETURNS TABLE(id bigint, workspace_id uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
    SELECT * FROM item WHERE id = iid;
$$ LANGUAGE plproxy;

------------------------------
-- Add item
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION add_item(uid uuid, workspace_id uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint)
RETURNS bigint AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

------------------------------
-- Add item id
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION add_item(uid uuid, iid bigint, workspace_id uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint)
RETURNS bigint AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;


------------------------------
-- Update item
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION update_item(uid uuid, wid uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint, item_id bigint)
RETURNS integer AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

------------------------------
-- Get items by workspace id
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION get_items_by_workspace_id(uid uuid, wid uuid)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, version_id bigint, version integer, is_folder boolean, workspace_id uuid, size bigint, status text, mimetype text, checksum bigint, device_id uuid, modified_at timestamp, level_array bigint[], chunks text[]) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
$$ LANGUAGE plproxy;

------------------------------
-- Get items by id
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION get_items_by_id(uid uuid, iid bigint)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, version_id bigint, version integer, is_folder boolean, workspace_id uuid, size bigint, status text, mimetype text, checksum bigint, device_id uuid, modified_at timestamp, level_array bigint[]) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
$$ LANGUAGE plproxy;

------------------------------
-- Find item by user id
------------------------------

CREATE OR REPLACE FUNCTION find_item_by_user_id(uid uuid)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, device_id uuid, workspace_id uuid, version integer, is_folder boolean, size bigint, status text, mimetype text, checksum bigint, modified_at timestamp, level_array bigint[], path text ) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
$$ LANGUAGE plproxy;

------------------------------
-- Find item versions by item id
------------------------------
item_id | parent_id | client_parent_file_version | filename | is_folder | mimetype | workspace_id | version | size | status | checksum | device_id | modified_at | level | path

--Proxy

CREATE OR REPLACE FUNCTION find_item_by_user_id(uid uuid, file_id bigint)
RETURNS TABLE(item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text,  is_folder boolean, mimetype text, workspace_id uuid, version integer, size bigint, status text,  checksum bigint, device_id uuid, modified_at timestamp, level integer, path text ) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
    SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, i.is_folder, i.mimetype, i.workspace_id, iv.version, iv.size, iv.status, iv.checksum, iv.device_id, iv.modified_at, '1' AS level, '' AS path FROM item i inner join item_version iv on iv.item_id = i.id where i.id = $2 ORDER BY iv.version DESC;
$$ LANGUAGE plproxy;

------------------------------
-- Migrate item
------------------------------

--Proxy

CREATE OR REPLACE FUNCTION migrate_item(uid uuid, iid bigint, wid uuid)
RETURNS INTEGER AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
$$ LANGUAGE plproxy;

------------------------------
-- Get chunks to migrate
------------------------------

--Proxy

CREATE OR REPLACE FUNCTION get_chunks_to_migrate(uid uuid, iid bigint)
RETURNS TABLE(chunks text[]) AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text);
    SELECT get_unique_chunks_to_migrate($2) AS chunks;
$$ LANGUAGE plproxy;

------------------------------
-- Insert user
------------------------------

-- Proxy # uuid_generate_v1()

CREATE OR REPLACE FUNCTION add_user(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer)
RETURNS SETOF integer AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(id::text) ;
$$ LANGUAGE plproxy;

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

------------------------------
-- Delete
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION delete_user(uid uuid)
RETURNS integer AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

------------------------------
-- Find user by itemid
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION find_user_by_item_id(id bigint)
RETURNS TABLE(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer, created_at timestamp) AS $$
    CLUSTER 'usercluster';
    RUN ON ALL;
$$ LANGUAGE plproxy;

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

------------------------------
-- Add workspace uuid
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION add_workspace(uid uuid, wid uuid, latest_revision text, owner_id uuid, is_shared boolean, is_encrypted boolean, swift_container text, swift_url text)
RETURNS uuid AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

------------------------------
-- Update workspace
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION update_workspace(uid uuid, wid uuid, workspace_name text, parent_item_id bigint)
RETURNS integer AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

------------------------------
-- Delete workspace
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION delete_workspace(uid uuid, wid uuid)
RETURNS integer AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

------------------------------
-- Add user into a workspace
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION add_user_workspace(uid uuid, wid uuid, workspace_name text, parent_item_id bigint)
RETURNS integer AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

------------------------------
-- Delete user into a workspace
------------------------------

-- Proxy

CREATE OR REPLACE FUNCTION delete_user_workspace(uid uuid, wid uuid)
RETURNS integer AS $$
	CLUSTER 'usercluster';
	RUN ON hashtext(uid::text) ;
$$ LANGUAGE plproxy;

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


-- Delete user1
CREATE OR REPLACE FUNCTION delete_user1()
RETURNS TABLE(result integer) AS $$
    CLUSTER 'usercluster';
    RUN ON ALL;
$$ LANGUAGE plproxy;

------------------------------
-- Do commit
------------------------------

CREATE OR REPLACE FUNCTION commit_object2(client_id uuid, it item, itv item_version, chunks item_version_chunk[])
RETURNS TABLE(item_id bigint, parent_id bigint, client_parent_file_version bigint, filename varchar(100), is_folder boolean, mimetype varchar(100), workspace_id uuid, version integer, device_id uuid, checksum bigint, status varchar(100), size bigint, modified_at timestamp, chunks text[]) AS $$
    CLUSTER 'usercluster';
    RUN ON hashtext(client_id::text) ;
$$ LANGUAGE plproxy;



