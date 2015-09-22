--
-- PostgreSQL database initialization
--

DROP TABLE IF EXISTS public.item_version_chunk, public.item_version, public.item, public.workspace_user, public.workspace, public.device, public.user1, public.attribute, public.abe_component CASCADE;
DROP SEQUENCE IF EXISTS public.sequencer_user, public.sequencer_workspace, public.sequencer_device, public.sequencer_item, public.sequencer_item_version, public.sequencer_chunk;

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

CREATE EXTENSION "uuid-ossp";

SET default_tablespace = '';
SET default_with_oids = false;



CREATE SEQUENCE public.sequencer_item
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
    
CREATE SEQUENCE public.sequencer_item_version
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
    
--
-- TABLE: user
--

CREATE TABLE public.user1 (
    id uuid NOT NULL default uuid_generate_v4(),
    name varchar(100) NOT NULL,
    swift_user varchar(100) NOT NULL UNIQUE,
    swift_account varchar(100) NOT NULL,
    email character varying(100) NOT NULL,
    quota_limit integer NOT NULL,
    quota_used integer DEFAULT 0 NOT NULL,
    created_at timestamp DEFAULT now()
);

ALTER TABLE public.user1 ADD CONSTRAINT pk_user PRIMARY KEY (id);


--
-- TABLE: device
--

CREATE TABLE public.device (
    id uuid NOT NULL default uuid_generate_v4(),
    name varchar(100) NOT NULL,
    user_id uuid,
    os varchar(100),
    created_at timestamp DEFAULT now(),
    last_access_at timestamp DEFAULT now(),
    last_ip inet,
    app_version varchar(45)
);

ALTER TABLE public.device ADD CONSTRAINT pk_device PRIMARY KEY (id);

ALTER TABLE public.device ADD CONSTRAINT fk1_device FOREIGN KEY (user_id) REFERENCES public.user1 (id) ON DELETE CASCADE;

INSERT INTO public.device ("id","name") VALUES ('00000000-0000-0001-0000-000000000001','API');
 

--
-- TABLE: workspace
--

CREATE TABLE public.workspace (
    id uuid NOT NULL default uuid_generate_v4(),
    latest_revision varchar(45) NOT NULL DEFAULT 0,
    owner_id uuid NOT NULL,
    is_shared boolean NOT NULL,
    is_encrypted boolean NOT NULL DEFAULT false,
    is_abe_encrypted boolean NOT NULL DEFAULT false,
    public_key bytea,
    swift_container varchar(45),
    swift_url varchar(250),
    created_at timestamp DEFAULT now()
);

ALTER TABLE public.workspace ADD CONSTRAINT pk_workspace PRIMARY KEY (id);

ALTER TABLE public.workspace ADD CONSTRAINT fk1_workspace FOREIGN KEY (owner_id) REFERENCES public.user1 (id) ON DELETE CASCADE;


--
-- TABLE: workspace_user
--

CREATE TABLE public.workspace_user (
    workspace_id uuid NOT NULL,
    user_id uuid NOT NULL,
    workspace_name varchar(255) NOT NULL,
    parent_item_id bigint,
    created_at timestamp DEFAULT now(),
    modified_at timestamp DEFAULT now(),
    secret_key bytea,
    access_struc bytea,
);

ALTER TABLE public.workspace_user ADD CONSTRAINT pk_workspace_user PRIMARY KEY (workspace_id, user_id);
ALTER TABLE public.workspace_user ADD CONSTRAINT fk1_workspace_user FOREIGN KEY (user_id) REFERENCES public.user1 (id) ON DELETE CASCADE;
ALTER TABLE public.workspace_user ADD CONSTRAINT fk2_workspace_user FOREIGN KEY (workspace_id) REFERENCES public.workspace (id) ON DELETE CASCADE;

CREATE TABLE public.workspace_attribute_universe (
    workspace_id uuid NOT NULL,
    attribute varchar(255) NOT NULL,
    id bigint NOT NULL UNIQUE
);

ALTER TABLE public.workspace_attribute_universe ADD CONSTRAINT pk_workspace_attribute_universe PRIMARY KEY (workspace_id, attribute);
ALTER TABLE public.workspace_attribute_universe ADD CONSTRAINT fk1_workspace_attribute_universe FOREIGN KEY (workspace_id) REFERENCES public.workspace (id) ON DELETE CASCADE;


--
-- TABLE: workspace_reencryption_key_history
--

CREATE TABLE public.workspace_reencryption_key_history (
    workspace_id uuid NOT NULL,
    attribute varchar(255) NOT NULL,
    version bigint NOT NULL,
    reencryption_key bytea
);

ALTER TABLE public.workspace_reencryption_key_history ADD CONSTRAINT pk_workspace_reencryption_history PRIMARY KEY (workspace_id, attribute, version);
ALTER TABLE public.workspace_reencryption_key_history ADD CONSTRAINT fk1_workspace_reencryption_key_history FOREIGN KEY (workspace_id, attribute) REFERENCES public.workspace_attribute_universe (workspace_id, attribute) ON DELETE CASCADE;

--
-- TABLE: workspace_reencryption_key_history
--

CREATE TABLE public.workspace_user_key_components (
    workspace_id uuid NOT NULL,
    user_id uuid NOT NULL,
    attribute varchar(255) NOT NULL,
    version bigint NOT NULL,
    component bytea NOT NULL
);

ALTER TABLE public.workspace_user_key_components ADD CONSTRAINT pk_workspace_user_key_components PRIMARY KEY (workspace_id, user_id, attribute);
ALTER TABLE public.workspace_user_key_components ADD CONSTRAINT fk1_workspace_user_key_components FOREIGN KEY (user_id) REFERENCES public.user1 (id) ON DELETE CASCADE;
ALTER TABLE public.workspace_user_key_components ADD CONSTRAINT fk2_workspace_user_key_components FOREIGN KEY (workspace_id, attribute) REFERENCES public.workspace_attribute_universe (workspace_id, attribute) ON DELETE CASCADE;

--
-- TABLE: item
-- NOTE that the added ABE field "encrypted_dek" can be null - in cases such as folders / non ABE-encrypted items
CREATE TABLE public.item (
    id bigint NOT NULL,
    workspace_id uuid NOT NULL,
    latest_version bigint NOT NULL,
    parent_id bigint,
    encrypted_dek bytea,
    filename varchar(100) NOT NULL,
    mimetype varchar(45) NOT NULL,
    is_folder boolean NOT NULL,
    updated boolean NOT NULL DEFAULT true,
    client_parent_file_version bigint
);

ALTER TABLE public.item ADD CONSTRAINT pk_item PRIMARY KEY (id);

ALTER SEQUENCE public.sequencer_item OWNED BY public.item.id;
ALTER TABLE ONLY public.item ALTER COLUMN id SET DEFAULT nextval('sequencer_item'::regclass);
ALTER TABLE public.item ADD CONSTRAINT fk1_item FOREIGN KEY (workspace_id) REFERENCES public.workspace (id) ON DELETE CASCADE;
ALTER TABLE public.item ADD CONSTRAINT fk2_item FOREIGN KEY (parent_id) REFERENCES public.item (id) ON DELETE CASCADE;
ALTER TABLE public.workspace_user ADD CONSTRAINT fk3_workspace_user FOREIGN KEY (parent_item_id) REFERENCES public.item (id);

CREATE INDEX item_workspace_id ON public.item (workspace_id);
CREATE INDEX item_parent_id ON public.item (parent_id);



--
-- TABLE: item_version
--

CREATE TABLE public.item_version (
    id bigint NOT NULL,
    item_id bigint NOT NULL,
    device_id uuid NOT NULL,
    version integer NOT NULL,
    committed_at timestamp,
    checksum bigint NOT NULL,
    modified_at timestamp,
    status varchar(10) NOT NULL, --- TODO: mirar si postgres tiene tipo enumerado
    size bigint NOT NULL
);

ALTER TABLE public.item_version ADD CONSTRAINT pk_item_version PRIMARY KEY (id);
ALTER SEQUENCE public.sequencer_item_version OWNED BY public.item_version.id;
ALTER TABLE ONLY public.item_version ALTER COLUMN id SET DEFAULT nextval('sequencer_item_version'::regclass);

ALTER TABLE public.item_version ADD CONSTRAINT fk2_item_version FOREIGN KEY (item_id) REFERENCES public.item (id) ON DELETE CASCADE;
ALTER TABLE public.item_version ADD CONSTRAINT fk3_item_version FOREIGN KEY (device_id) REFERENCES public.device (id) ON DELETE CASCADE;

CREATE INDEX item_version_item_id ON public.item_version(item_id);

CREATE UNIQUE INDEX itemid_version_unique ON public.item_version (item_id, version);


--
-- TABLE: item_version_chunk
--

CREATE TABLE public.item_version_chunk (
    item_version_id bigint NOT NULL,
	client_chunk_name character varying(40) NOT NULL,
    chunk_order integer NOT NULL
);

ALTER TABLE public.item_version_chunk ADD CONSTRAINT pk_item_version_chunk PRIMARY KEY (item_version_id, client_chunk_name, chunk_order);
ALTER TABLE public.item_version_chunk ADD CONSTRAINT fk2_item_version_chunk FOREIGN KEY (item_version_id) REFERENCES public.item_version (id) ON DELETE CASCADE;

--
-- TABLE: curve
--

-- CREATE TABLE public.curve (
--     id uuid NOT NULL default uuid_generate_v4(),
--     type varchar(20) NOT NULL,
--     q varchar(200) NOT NULL,
--     r varchar(200) NOT NULL,
--     h varchar(200) NOT NULL,
--     exp1 integer NOT NULL,
--     exp2 integer NOT NULL,
--     sign0 integer NOT NULL,
--     sign1 integer NOT NULL
-- );
-- 
-- ALTER TABLE public.curve ADD CONSTRAINT pk_curve PRIMARY KEY (id);

--
-- TABLE: attribute
--
CREATE TABLE public.attribute (
    id uuid NOT NULL default uuid_generate_v4()
--     name varchar(100) NOT NULL,
--     latest_version bigint NOT NULL,
--     public_key_component varchar(500) NOT NULL,
--     history_list json
);

ALTER TABLE public.attribute ADD CONSTRAINT pk_attribute PRIMARY KEY (id);

--
-- TABLE: access_component
--
-- [revocation] set user_id field type to UUID
-- CREATE TABLE public.access_component (
--     id uuid NOT NULL default uuid_generate_v4(),
--     user_id character varying(100) NOT NULL,
--     attribute uuid NOT NULL,
--     sk_component varchar(500) NOT NULL,
--     version integer NOT NULL
-- );
-- 
-- ALTER TABLE public.access_component ADD CONSTRAINT pk_access_component PRIMARY KEY (id);
-- ALTER TABLE public.access_component ADD CONSTRAINT fk1_access_component FOREIGN KEY (user_id) REFERENCES public.user1 (id) ON DELETE CASCADE;
-- ALTER TABLE public.access_component ADD CONSTRAINT fk2_access_component FOREIGN KEY (attribute) REFERENCES public.attribute (id) ON DELETE CASCADE;

--
-- TABLE: abe_component
--
-- NOTE: on bring up - set FK to item
CREATE TABLE public.abe_component (
    item_id bigint NOT NULL,
    attribute character varying(255) NOT NULL,
    version bigint NOT NULL,
    encrypted_pk_component bytea NOT NULL,
    item_version bigint NOT NULL
);

ALTER TABLE public.abe_component ADD CONSTRAINT pk_abe_component PRIMARY KEY (item_id, attribute, version);
ALTER TABLE public.abe_component ADD CONSTRAINT fk1_abe_component FOREIGN KEY (item_id) REFERENCES public.item (id) ON DELETE CASCADE;

--
-- FUNCTIONS
--

-- Returns the path given a item_id
CREATE OR REPLACE FUNCTION get_path(bigint, OUT result text)
  RETURNS text AS
$BODY$
BEGIN

	WITH RECURSIVE q AS 
	( 
		SELECT i.id, i.parent_id, ARRAY[i.id] AS level_array, '/' AS path
		FROM item i 
		WHERE i.id = $1
		UNION ALL 
		SELECT i2.id, i2.parent_id, q.level_array || i2.id,  '/' || i2.filename::TEXT || q.path
		FROM q 
		JOIN item i2 ON i2.id = q.parent_id 
		)
	SELECT INTO result path
	FROM (
		SELECT array_upper(level_array, 1) as level, q.path 
		FROM q 
		ORDER BY level DESC
		LIMIT 1
	) as foo;

END;
$BODY$
  LANGUAGE plpgsql VOLATILE;

  
-- Returns an array of chunks corresponding to the given item_version_id
CREATE OR REPLACE FUNCTION get_chunks(bigint, OUT result text[])
  RETURNS text[] AS
$BODY$
BEGIN

--array_cat: appends an empty array to avoid null values when no chunks are found

    SELECT INTO result array_cat(ARRAY[]::character varying[], array_agg(client_chunk_name)) AS chunks
    FROM
    (
        SELECT ivc.client_chunk_name
        FROM item_version_chunk ivc
        WHERE ivc.item_version_id = $1
        ORDER BY ivc.chunk_order ASC
    ) AS foo;

END;
$BODY$
  LANGUAGE plpgsql VOLATILE;


-- Returns an array of chunks corresponding to the given item_id and its childen
CREATE OR REPLACE FUNCTION get_unique_chunks_to_migrate(bigint, OUT result text[])
  RETURNS text[] AS
$BODY$
BEGIN
	
WITH    RECURSIVE 
q AS  
(  
    SELECT i.id, ivc.client_chunk_name
    FROM    item i 
    INNER JOIN item_version iv ON i.id = iv.item_id AND i.latest_version = iv.version
    LEFT JOIN item_version_chunk ivc ON iv.id = ivc.item_version_id
    WHERE   i.id = $1 
    UNION ALL 
    SELECT i2.id, ivc2.client_chunk_name
    FROM    q 
    JOIN    item i2 ON i2.parent_id = q.id 
    INNER JOIN item_version iv2 ON i2.id = iv2.item_id AND i2.latest_version = iv2.version
    LEFT JOIN item_version_chunk ivc2 ON iv2.id = ivc2.item_version_id
)
SELECT INTO result array_agg(client_chunk_name) AS chunks
FROM
(
    SELECT DISTINCT client_chunk_name
    FROM q
    where q.client_chunk_name != ''
) as a1;

END
$BODY$
  LANGUAGE plpgsql VOLATILE;
