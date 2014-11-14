WITH RECURSIVE q AS 
	(
	SELECT i.id AS item_id, i.parent_id, i.client_parent_file_version,
		i.filename, iv.id AS version_id, iv.version, i.is_folder, i.encrypted_dek,
		i.workspace_id, 
		iv.size, iv.status, i.mimetype, 
		iv.checksum, iv.device_id, iv.modified_at, 
		ARRAY[i.id] AS level_array 
	FROM workspace w  
	INNER JOIN item i ON w.id = i.workspace_id 
	INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version 
	WHERE w.id = 'a0d404a4-7240-4750-89b2-3c01601b0c1d'::uuid AND i.parent_id IS NULL 
	UNION ALL  
	SELECT i2.id AS item_id, i2.parent_id, i2.client_parent_file_version, 
		i2.filename, iv2.id AS version_id, iv2.version, i2.is_folder, i2.encrypted_dek,
		i2.workspace_id, 
		iv2.size, iv2.status, i2.mimetype, 
		iv2.checksum, iv2.device_id, iv2.modified_at, 
		q.level_array || i2.id 
	FROM q  
	JOIN item i2 ON i2.parent_id = q.item_id
	INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version
	WHERE i2.workspace_id='a0d404a4-7240-4750-89b2-3c01601b0c1d'::uuid 
	)  
	SELECT array_upper(level_array, 1) as level, 
	c.id as abemeta_id, c.item_id, c.attribute, c.encrypted_pk_component, c.version, 
	q.*, get_chunks(q.version_id) AS chunks 
	FROM q
	LEFT OUTER JOIN abe_component c
	ON c.item_id = q.item_id
	ORDER BY level_array ASC