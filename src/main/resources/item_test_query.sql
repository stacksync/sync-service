WITH RECURSIVE q AS 
	(
	SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version,
		i.filename, iv.id AS version_id, iv.version, i.is_folder,
		i.workspace_id,
		iv.size, iv.status, i.mimetype, 
		iv.checksum, iv.device_id, iv.modified_at,
		ARRAY[i.id] AS level_array 
	FROM item i 
	INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version 
	WHERE i.id = 2 
	UNION ALL 
	SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version, 
		i2.filename, iv2.id AS version_id, iv2.version, i2.is_folder,
		i2.workspace_id, 
		iv2.size, iv2.status, i2.mimetype, 
		iv2.checksum, iv2.device_id, iv2.modified_at, 
		q.level_array || i2.id 
	FROM q 
	JOIN item i2 ON i2.parent_id = q.item_id 
	INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version 
	) 
	SELECT array_upper(level_array, 1) as level, q.* 
	FROM q 
	ORDER BY 
	level_array ASC
