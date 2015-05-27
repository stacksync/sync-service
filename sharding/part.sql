
------------------------------
-- Add device
------------------------------

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
-- Add device uuid
------------------------------

CREATE OR REPLACE FUNCTION add_device(uid uuid, did uuid, name text, os text, last_ip inet, app_version text)
RETURNS uuid AS $$
BEGIN
	INSERT INTO device (id, name, user_id, os, created_at, last_access_at, last_ip, app_version) VALUES (did, $3, $1, $4, now(), now(), $5, $6);
	return did;
END;
$$ LANGUAGE plpgsql;

------------------------------
-- Update device
------------------------------

-- Part

CREATE OR REPLACE FUNCTION update_device(uid uuid, did uuid, last_ip inet, app_version text)
RETURNS integer AS $$
	UPDATE device SET last_access_at = now(), last_ip = $3, app_version = $4 WHERE id = $2 and user_id = $1;
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Delete device
------------------------------

-- Part

CREATE OR REPLACE FUNCTION delete_device(uid uuid, did uuid)
RETURNS integer AS $$
	DELETE FROM device WHERE id = $2;
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Add item version
------------------------------

-- Part

CREATE OR REPLACE FUNCTION add_item_version(uid uuid, item_id bigint, device_id uuid, version bigint, checksum bigint, status text, size bigint, modified_at timestamp)
RETURNS bigint AS $$
    INSERT INTO item_version( item_id, device_id, version, checksum, status, size, modified_at, committed_at) VALUES ( $2, $3, $4, $5, $6, $7, $8, now()) RETURNING id;
$$ LANGUAGE SQL;



------------------------------
-- Insert chunk
------------------------------

-- Part
CREATE OR REPLACE FUNCTION insert_chunk(uid uuid, item_version_id bigint, client_chunk_name text, chunk_order integer)
RETURNS integer AS $$
    INSERT INTO item_version_chunk( item_version_id, client_chunk_name, chunk_order ) VALUES ( $2, $3, $4 );
$$ LANGUAGE SQL;

------------------------------
-- Insert chunks // this function is built dynamically
------------------------------

-- Part

CREATE OR REPLACE FUNCTION dynamic_query(uid uuid, sql text)
RETURNS void AS $$
BEGIN
	EXECUTE sql;
END;
$$ LANGUAGE plpgsql;

-- Part

CREATE OR REPLACE FUNCTION dynamic_void_query(uid uuid, sql text)
RETURNS void AS $$
BEGIN
	EXECUTE sql;
END;
$$ LANGUAGE plpgsql;


------------------------------
-- Add item
------------------------------

-- Part
CREATE OR REPLACE FUNCTION add_item(uid uuid, workspace_id uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint)
RETURNS bigint AS $$
DECLARE
    iid bigint;
BEGIN
    INSERT INTO item (workspace_id, latest_version, parent_id, filename, mimetype, is_folder, client_parent_file_version ) VALUES ( $2, $3, $4, $5, $6, $7, $8) RETURNING id INTO iid;
    RETURN iid;
END;
$$ LANGUAGE plpgsql;

------------------------------
-- Add item id
------------------------------

-- Part
CREATE OR REPLACE FUNCTION add_item(uid uuid, iid bigint, workspace_id uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint)
RETURNS bigint AS $$
BEGIN
    INSERT INTO item (id, workspace_id, latest_version, parent_id, filename, mimetype, is_folder, client_parent_file_version ) VALUES ( $2, $3, $4, $5, $6, $7, $8, $9) RETURNING id INTO iid;
    RETURN iid;
END;
$$ LANGUAGE plpgsql;


------------------------------
-- Update item
------------------------------

-- Part

CREATE OR REPLACE FUNCTION update_item(uid uuid, wid uuid, latest_version bigint, parent_id bigint, filename text, mimetype text, is_folder boolean, client_parent_file_version bigint, item_id bigint)
RETURNS integer AS $$
    UPDATE item SET workspace_id = $2, latest_version = $3, parent_id = $4, filename = $5, mimetype = $6, is_folder = $7, client_parent_file_version = $8 WHERE id = $9;
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Get items by workspace id
------------------------------

-- Part

CREATE OR REPLACE FUNCTION get_items_by_workspace_id(uid uuid, wid uuid)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, version_id bigint, version integer, is_folder boolean, workspace_id uuid, size bigint, status text, mimetype text, checksum bigint, device_id uuid, modified_at timestamp, level_array bigint[], chunks text[]) AS $$
    WITH RECURSIVE q AS (SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, iv.id AS version_id, iv.version, i.is_folder, i.workspace_id, iv.size, iv.status, i.mimetype, iv.checksum, iv.device_id, iv.modified_at, ARRAY[i.id] AS level_array FROM workspace w INNER JOIN item i ON w.id = i.workspace_id INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version WHERE w.id = $2 AND i.parent_id IS NULL UNION ALL SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version, i2.filename, iv2.id AS version_id, iv2.version, i2.is_folder, i2.workspace_id, iv2.size, iv2.status, i2.mimetype, iv2.checksum, iv2.device_id, iv2.modified_at, q.level_array || i2.id FROM q JOIN item i2 ON i2.parent_id = q.item_id INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version WHERE i2.workspace_id=$2 ) SELECT array_upper(level_array, 1) as level, q.*, get_chunks(q.version_id) AS chunks FROM q ORDER BY level_array ASC;
$$ LANGUAGE SQL;

------------------------------
-- Get items by id
------------------------------


-- Part

CREATE OR REPLACE FUNCTION get_items_by_id(uid uuid, iid bigint)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, version_id bigint, version integer, is_folder boolean, workspace_id uuid, size bigint, status text, mimetype text, checksum bigint, device_id uuid, modified_at timestamp, level_array bigint[]) AS $$
    WITH RECURSIVE q AS (SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, iv.id AS version_id, iv.version, i.is_folder, i.workspace_id, iv.size, iv.status, i.mimetype, iv.checksum, iv.device_id, iv.modified_at, ARRAY[i.id] AS level_array FROM item i INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version WHERE i.id = $2 UNION ALL SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version, i2.filename, iv2.id AS version_id, iv2.version, i2.is_folder, i2.workspace_id, iv2.size, iv2.status, i2.mimetype, iv2.checksum, iv2.device_id, iv2.modified_at, q.level_array || i2.id FROM q JOIN item i2 ON i2.parent_id = q.item_id INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version) SELECT array_upper(level_array, 1) as level, q.* FROM q ORDER BY level_array ASC;
$$ LANGUAGE SQL;


------------------------------
-- Find item by user id
------------------------------

-- Part

CREATE OR REPLACE FUNCTION find_item_by_user_id(uid uuid)
RETURNS TABLE(level integer, item_id bigint, parent_id bigint, client_parent_file_version bigint, filename text, device_id uuid, workspace_id uuid, version integer, is_folder boolean, size bigint, status text, mimetype text, checksum bigint, modified_at timestamp, level_array bigint[], path text ) AS $$
    WITH RECURSIVE q AS (SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, iv.device_id, i.workspace_id, iv.version, i.is_folder, iv.size, iv.status, i.mimetype, iv.checksum, iv.modified_at, ARRAY[i.id] AS level_array, '/' AS path FROM user1 u INNER JOIN workspace_user wu ON u.id = wu.user_id INNER JOIN item i ON wu.workspace_id = i.workspace_id INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version WHERE u.id = $1 AND i.parent_id IS NULL UNION ALL SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version, i2.filename, iv2.device_id, i2.workspace_id, iv2.version, i2.is_folder, iv2.size, iv2.status, i2.mimetype, iv2.checksum, iv2.modified_at, q.level_array || i2.id, q.path || q.filename::TEXT || '/' FROM q JOIN item i2 ON i2.parent_id = q.item_id INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version WHERE array_upper(level_array, 1) < 1) SELECT array_upper(level_array, 1) as level, q.* FROM q ORDER BY level_array ASC;

$$ LANGUAGE SQL;


------------------------------
-- Migrate item
------------------------------

-- Part

CREATE OR REPLACE FUNCTION migrate_item(uid uuid, iid bigint, wid uuid)
RETURNS INTEGER AS $$
    WITH RECURSIVE q AS ( SELECT i.* FROM item i WHERE i.id = $2 UNION ALL SELECT i2.* FROM q JOIN item i2 ON i2.parent_id = q.id ) UPDATE item i3 SET workspace_id = $3 FROM q WHERE q.id = i3.id;
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Insert user
------------------------------

-- Part
CREATE OR REPLACE FUNCTION add_user(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer)
RETURNS integer AS $$
	INSERT INTO user1 (id, email, name, swift_user, swift_account, quota_limit, quota_used) VALUES ($1, $2, $3, $4, $5, $6, $7);
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Update
------------------------------

-- Part

CREATE OR REPLACE FUNCTION update_user(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer)
RETURNS integer AS $$
	UPDATE user1 SET email = $2, name = $3, swift_user = $4, swift_account = $5, quota_limit = $6, quota_used = $7 WHERE id = $1;
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Delete
------------------------------

-- Part

CREATE OR REPLACE FUNCTION delete_user(uid uuid)
RETURNS integer AS $$
	DELETE FROM user1 WHERE id = $1;
	SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Find user by itemid
------------------------------

-- Part

CREATE OR REPLACE FUNCTION find_user_by_item_id(id bigint)
RETURNS TABLE(id uuid, email text, name text, swift_user text, swift_account text, quota_limit integer, quota_used integer, created_at timestamp) AS $$
	SELECT u.* FROM item i INNER JOIN workspace_user wu ON i.workspace_id = wu.workspace_id INNER JOIN user1 u ON wu.user_id = u.id WHERE i.id = $1;
$$ LANGUAGE SQL;

------------------------------
-- Add workspace
------------------------------

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
-- Add workspace uuid
------------------------------

-- Part
CREATE OR REPLACE FUNCTION add_workspace(uid uuid, wid uuid, latest_revision text, owner_id uuid, is_shared boolean, is_encrypted boolean, swift_container text, swift_url text)
RETURNS uuid AS $$
BEGIN
    INSERT INTO workspace (id, latest_revision, owner_id, is_shared, is_encrypted, swift_container, swift_url) VALUES (wid, $3, $4, $5, $6, $7, $8);
    INSERT INTO workspace_user (workspace_id, user_id, workspace_name, parent_item_id) VALUES (wid, uid, 'default', NULL);
    return wid;
END;
$$ LANGUAGE plpgsql;

------------------------------
-- Update workspace
------------------------------

-- Part

CREATE OR REPLACE FUNCTION update_workspace(uid uuid, wid uuid, workspace_name text, parent_item_id bigint)
RETURNS integer AS $$
    UPDATE workspace_user SET workspace_name = $3, parent_item_id = $4, modified_at = now() WHERE user_id = $1 AND workspace_id = $2;
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Delete workspace
------------------------------

-- Part

CREATE OR REPLACE FUNCTION delete_workspace(uid uuid, wid uuid)
RETURNS integer AS $$
    DELETE FROM workspace WHERE id = $2;
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Add user into a workspace
------------------------------

-- Part

CREATE OR REPLACE FUNCTION add_user_workspace(uid uuid, wid uuid, workspace_name text, parent_item_id bigint)
RETURNS integer AS $$
    INSERT INTO workspace_user (workspace_id, user_id, workspace_name, parent_item_id) VALUES ($2, $1, $3, $4);
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Delete user into a workspace
------------------------------

-- Part

CREATE OR REPLACE FUNCTION delete_user_workspace(uid uuid, wid uuid)
RETURNS integer AS $$
    DELETE FROM workspace_user WHERE user_id=$1 AND workspace_id=$2;
    SELECT 1;
$$ LANGUAGE SQL;


-- Delete user1
CREATE OR REPLACE FUNCTION delete_user1()
RETURNS integer AS $$
    DELETE FROM user1;
    SELECT 1;
$$ LANGUAGE SQL;

------------------------------
-- Do commit
------------------------------


CREATE OR REPLACE FUNCTION commit_object2(client_id uuid, it item, itv item_version, item_chunks item_version_chunk[])
RETURNS TABLE(item_id bigint, parent_id bigint, client_parent_file_version bigint, filename varchar(100), is_folder boolean, mimetype varchar(100), workspace_id uuid, version integer, device_id uuid, checksum bigint, status varchar(100), size bigint, modified_at timestamp, chunks text[]) AS $$
DECLARE
    server_item item;
    x item_version_chunk;
BEGIN
    
    -- check if this object already exists in the server.
    SELECT * INTO server_item FROM item WHERE id = it.id;
    
    IF NOT FOUND THEN
        IF it.latest_version = 1 THEN
            RAISE NOTICE 'version == 1';
            RETURN QUERY SELECT * from save_new_object2(it, itv, item_chunks);
        ELSE
            RAISE EXCEPTION 'Wrong version no parent'; 
        END IF;

        RETURN;
    END IF;

    -- check if the client version already exists in the server
    IF server_item.latest_version >= it.latest_version THEN
        --save_existent_version(serveritem, item);
        RAISE NOTICE 'save_existent_version(serveritem, item)';        
        RETURN QUERY SELECT * from save_existent_version2(it, itv);
    ELSE
        -- check if version is correct
        IF server_item.latest_version + 1 = it.latest_version THEN
            -- save_new_version(item, serveritem, workspace, device)
            RAISE NOTICE 'save_new_version(serveritem, item)';
            RETURN QUERY SELECT * from save_new_version2(it, itv, item_chunks);
        ELSE
            RAISE NOTICE 'invalid version';
            RAISE EXCEPTION 'Invalid version --> %', it.latest_version USING HINT = 'Please check your item version'; 
        END IF;
    END IF;
    
    RETURN;
END;
$$ LANGUAGE plpgsql;



CREATE OR REPLACE FUNCTION save_new_object2(it item, itv item_version, item_chunks item_version_chunk[])
RETURNS TABLE(item_id bigint, parent_id bigint, client_parent_file_version bigint, filename varchar(100), is_folder boolean, mimetype varchar(100), workspace_id uuid, version integer, device_id uuid, checksum bigint, status varchar(100), size bigint, modified_at timestamp, chunks text[]) AS $$
DECLARE
    parent_item item;
    iid bigint;    
    item_version_id bigint;
    x item_version_chunk;
BEGIN
    IF it.parent_id != null THEN
        SELECT * INTO parent_item FROM item WHERE id = it.parent_id;
    END IF;

    RAISE NOTICE 'BEFORE INSERT INTO item';

    INSERT INTO item (workspace_id, latest_version, parent_id, filename, mimetype, is_folder, client_parent_file_version ) VALUES ( it.workspace_id, it.latest_version, it.parent_id, it.filename, it.mimetype, it.is_folder, it.client_parent_file_version ) RETURNING id INTO iid;
        
    it.id := iid;
    itv.item_id := iid;

    RAISE NOTICE 'AFTER INSERT INTO item, it.id = %', it.id;

    -- insert object version
    INSERT INTO item_version(item_id, device_id, version, checksum, status, size, modified_at, committed_at) VALUES ( itv.item_id, itv.device_id, itv.version, itv.checksum, itv.status, itv.size, itv.modified_at, now()) RETURNING id INTO item_version_id;
    
    itv.id := item_version_id;
    
    -- if it isn't a folder, create new chunks
    IF NOT it.is_folder THEN
        FOREACH x IN ARRAY item_chunks
          LOOP
            -- item_version_id cannot be extracted from x.item_version_id!!
            INSERT INTO item_version_chunk (item_version_id, client_chunk_name, chunk_order) VALUES (itv.id, x.client_chunk_name, x.chunk_order);
        END LOOP;
    END IF;


    
    RETURN QUERY SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, i.is_folder, i.mimetype, i.workspace_id, iv.version, iv.device_id, iv.checksum, iv.status, iv.size, iv.modified_at, get_chunks(iv.id) AS chunks FROM item_version iv INNER JOIN item i ON i.id = iv.item_id WHERE iv.item_id = it.id and iv.version = itv.version;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION save_new_version2(it item, itv item_version, item_chunks item_version_chunk[])
RETURNS TABLE(item_id bigint, parent_id bigint, client_parent_file_version bigint, filename varchar(100), is_folder boolean, mimetype varchar(100), workspace_id uuid, version integer, device_id uuid, checksum bigint, status varchar(100), size bigint, modified_at timestamp, chunks text[]) AS $$
DECLARE
    item_version_id bigint;
    x item_version_chunk;
BEGIN
    -- insert object version
    INSERT INTO item_version(item_id, device_id, version, checksum, status, size, modified_at, committed_at) VALUES ( itv.item_id, itv.device_id, itv.version, itv.checksum, itv.status, itv.size, itv.modified_at, now()) RETURNING id INTO item_version_id;
    
    itv.id := item_version_id;
    
    -- if it isn't a folder, create new chunks
    IF NOT it.is_folder THEN
        FOREACH x IN ARRAY item_chunks
          LOOP
            INSERT INTO item_version_chunk (item_version_id, client_chunk_name, chunk_order) VALUES (itv.id, x.client_chunk_name, x.chunk_order);
        END LOOP;
    END IF;    

    IF itv.status = 'RENAMED' OR itv.status = 'MOVED' OR itv.status = 'DELETED' THEN
        IF it.parent_id = NULL THEN
            it.client_parent_file_version := null;
        --ELSE
            --SELECT * INTO parent_item FROM item WHERE id = it.parent_id;
            --it.parent_id := parent_item.id
        END IF;
    END IF;

    -- update object latest version
    -- serverItem.setLatestVersion(metadata.getVersion());
    -- itemDao.put(serverItem);
    UPDATE item SET workspace_id = it.workspace_id, latest_version = it.latest_version, parent_id = it.parent_id, filename = it.filename, mimetype = it.mimetype, is_folder = it.is_folder, client_parent_file_version = it.client_parent_file_version WHERE id = it.id;

    RETURN QUERY SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version, i.filename, i.is_folder, i.mimetype, i.workspace_id, iv.version, iv.device_id, iv.checksum, iv.status, iv.size, iv.modified_at, get_chunks(iv.id) AS chunks FROM item_version iv INNER JOIN item i ON i.id = iv.item_id WHERE iv.item_id = it.id and iv.version = itv.version;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION save_existent_version2(it item, itv item_version)
RETURNS void AS $$
DECLARE
    it2 item;
    itv2 item_version;
BEGIN

    SELECT * INTO itv2 FROM item_version WHERE item_id = it.id and version = it.latest_version;
    
    IF itv.device_id != itv2.device_id OR itv.version != itv2.version OR itv.checksum != itv2.checksum OR itv.status != status OR itv.size != itv2.size THEN
        RAISE EXCEPTION 'Invalid version --> %', it.latest_version USING HINT = 'Please check your item version'; 
    END IF;

    SELECT * INTO it2 FROM item WHERE id = it.id;

    IF it.id != it2.id    OR it.latest_version != it2.latest_version THEN
        RAISE EXCEPTION 'This version already exists --> %', it.latest_version USING HINT = 'Please check your item version'; 
    END IF;    

    RETURN;
END;
$$ LANGUAGE plpgsql;


