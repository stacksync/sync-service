--
-- PostgreSQL database initialization
--

DROP TABLE IF EXISTS public.item_version_chunk, public.item_version, public.item, public.workspace_user, public.workspace, public.device, public.user1, public.oauth1_access_tokens, public.oauth1_consumers, public.oauth1_nonce, public.oauth1_request_tokens CASCADE;
DROP SEQUENCE IF EXISTS public.sequencer_user, public.sequencer_workspace, public.sequencer_device, public.sequencer_item, public.sequencer_item_version, public.sequencer_chunk, public.oauth1_access_tokens_id_seq, public.oauth1_consumers_id_seq, public.oauth1_nonce_id_seq, public.oauth1_request_tokens_id_seq;

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
    quota_limit bigint NOT NULL,
    quota_used_logical bigint DEFAULT 0 NOT NULL,
    quota_used_real bigint DEFAULT 0 NOT NULL,
    created_at timestamp DEFAULT now()
);

ALTER TABLE public.user1 ADD CONSTRAINT pk_user PRIMARY KEY (id);
COPY user1 (id, name, swift_user, swift_account, email, quota_limit, quota_used_logical, quota_used_real, created_at) FROM stdin;
9db83ed6-c22f-4bef-905f-4e4af931d92b	web	none	none	none	0	0	0	2015-04-15 18:15:22.179898
\.


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

--INSERT INTO public.device ("id","name") VALUES ('00000000-0000-0001-0000-000000000001','API');

COPY device (id, name, user_id, os, created_at, last_access_at, last_ip, app_version) FROM stdin;
00000000-0000-0001-0000-000000000001	web	9db83ed6-c22f-4bef-905f-4e4af931d92b	web	2014-04-09 17:14:33.530998	2014-04-09 17:14:33.530998	\N	\N
\.
 

--
-- TABLE: workspace
--

CREATE TABLE public.workspace (
    id uuid NOT NULL default uuid_generate_v4(),
    latest_revision varchar(45) NOT NULL DEFAULT 0,
    owner_id uuid NOT NULL,
    is_shared boolean NOT NULL,
    is_encrypted boolean NOT NULL DEFAULT false,
    swift_container varchar(45),
    swift_url varchar(250),
    created_at timestamp DEFAULT now()
);

ALTER TABLE public.workspace ADD CONSTRAINT pk_workspace PRIMARY KEY (id);

ALTER TABLE public.workspace ADD CONSTRAINT fk1_workspace FOREIGN KEY (owner_id) REFERENCES public.user1 (id) ON DELETE CASCADE;

INSERT INTO workspace (id, latest_revision, owner_id, is_shared, is_encrypted, swift_container, swift_url) values ('07fd5785-f148-4e24-bd22-195e6bc78fe4', 0, '9db83ed6-c22f-4bef-905f-4e4af931d92b', false, false, 'no_swift_container', 'no_swift_url');
--COPY workspace (id, latest_revision, owner_id, is_shared, is_encrypted, swift_container, swift_url) FROM stdin;
--07fd5785-f148-4e24-bd22-195e6bc78fe4	0	9db83ed6-c22f-4bef-905f-4e4af931d92b	f	f	no_swift_container  no_swift_url
--\.


--
-- TABLE: workspace_user
--

CREATE TABLE public.workspace_user (
    id uuid,
    workspace_id uuid NOT NULL,
    user_id uuid NOT NULL,
    workspace_name varchar(255) NOT NULL,
    parent_item_id bigint,
    created_at timestamp DEFAULT now(),
    modified_at timestamp DEFAULT now()
);

ALTER TABLE public.workspace_user ADD CONSTRAINT pk_workspace_user PRIMARY KEY (workspace_id, user_id);
ALTER TABLE public.workspace_user ADD CONSTRAINT fk1_workspace_user FOREIGN KEY (user_id) REFERENCES public.user1 (id) ON DELETE CASCADE;
ALTER TABLE public.workspace_user ADD CONSTRAINT fk2_workspace_user FOREIGN KEY (workspace_id) REFERENCES public.workspace (id) ON DELETE CASCADE;

COPY workspace_user (workspace_id, user_id, workspace_name, parent_item_id, created_at, modified_at, id) FROM stdin;
07fd5785-f148-4e24-bd22-195e6bc78fe4	9db83ed6-c22f-4bef-905f-4e4af931d92b	default	\N	2014-04-09 17:14:11.16674	2014-04-09 17:14:11.16674	\N
\.


--
-- TABLE: item
--

CREATE TABLE public.item (
    id bigint NOT NULL,
    workspace_id uuid NOT NULL,
    latest_version bigint NOT NULL,
    parent_id bigint,
    filename varchar(100) NOT NULL,
    mimetype varchar(150) NOT NULL,
    is_folder boolean NOT NULL,
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
    client_chunk_name character varying(80) NOT NULL,
    chunk_order integer NOT NULL
);

ALTER TABLE public.item_version_chunk ADD CONSTRAINT pk_item_version_chunk PRIMARY KEY (item_version_id, client_chunk_name, chunk_order);
ALTER TABLE public.item_version_chunk ADD CONSTRAINT fk2_item_version_chunk FOREIGN KEY (item_version_id) REFERENCES public.item_version (id) ON DELETE CASCADE;


--
-- OAUTH TABLES
--

--
-- Name: oauth1_request_tokens;
--

CREATE TABLE oauth1_request_tokens (
    id integer NOT NULL,
    consumer integer,
    "user" uuid,
    realm character varying,
    redirect_uri character varying,
    request_token character varying,
    request_token_secret character varying,
    verifier character varying,
    created_at timestamp without time zone,
    modified_at timestamp without time zone
);

CREATE SEQUENCE oauth1_request_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE oauth1_request_tokens_id_seq OWNED BY oauth1_request_tokens.id;
ALTER TABLE ONLY oauth1_request_tokens ALTER COLUMN id SET DEFAULT nextval('oauth1_request_tokens_id_seq'::regclass);
ALTER TABLE ONLY oauth1_request_tokens ADD CONSTRAINT oauth1_request_tokens_pkey PRIMARY KEY (id);
ALTER TABLE ONLY oauth1_request_tokens ADD CONSTRAINT oauth1_request_tokens_user_fkey FOREIGN KEY ("user") REFERENCES user1(id);

--
-- Name: oauth1_consumers;
--

CREATE TABLE oauth1_consumers (
    id integer NOT NULL,
    consumer_key character varying,
    consumer_secret character varying,
    rsa_key character varying,
    "user" uuid,
    realm character varying,
    redirect_uri character varying,
    application_title character varying,
    application_description character varying,
    application_uri character varying,
    created_at timestamp without time zone,
    modified_at timestamp without time zone
);

CREATE SEQUENCE oauth1_consumers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE oauth1_consumers_id_seq OWNED BY oauth1_consumers.id;
ALTER TABLE ONLY oauth1_consumers ALTER COLUMN id SET DEFAULT nextval('oauth1_consumers_id_seq'::regclass);
ALTER TABLE ONLY oauth1_consumers ADD CONSTRAINT oauth1_consumers_pkey PRIMARY KEY (id);
ALTER TABLE ONLY oauth1_consumers ADD CONSTRAINT oauth1_consumers_user_fkey FOREIGN KEY ("user") REFERENCES user1(id);
ALTER TABLE ONLY oauth1_request_tokens ADD CONSTRAINT oauth1_request_tokens_consumer_fkey FOREIGN KEY (consumer) REFERENCES oauth1_consumers(id);

COPY oauth1_consumers (id, consumer_key, consumer_secret, rsa_key, "user", realm, redirect_uri, application_title, application_description, application_uri, created_at, modified_at) FROM stdin;
1	b3af4e669daf880fb16563e6f36051b105188d413	c168e65c18d75b35d8999b534a3776cf	\N	9db83ed6-c22f-4bef-905f-4e4af931d92b	stacksync	oob	StackSync Test App	This is an application test	http://example.com	\N	\N
\.

SELECT pg_catalog.setval('oauth1_consumers_id_seq', 1, true);

--
-- Name: oauth1_access_tokens;
--

CREATE TABLE oauth1_access_tokens (
    id integer NOT NULL,
    consumer integer,
    "user" uuid,
    realm character varying,
    access_token character varying,
    access_token_secret character varying,
    created_at timestamp without time zone,
    modified_at timestamp without time zone
);

CREATE SEQUENCE oauth1_access_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE oauth1_access_tokens_id_seq OWNED BY oauth1_access_tokens.id;
ALTER TABLE ONLY oauth1_access_tokens ALTER COLUMN id SET DEFAULT nextval('oauth1_access_tokens_id_seq'::regclass);
ALTER TABLE ONLY oauth1_access_tokens ADD CONSTRAINT oauth1_access_tokens_pkey PRIMARY KEY (id);
ALTER TABLE ONLY oauth1_access_tokens ADD CONSTRAINT oauth1_access_tokens_consumer_fkey FOREIGN KEY (consumer) REFERENCES oauth1_consumers(id);
ALTER TABLE ONLY oauth1_access_tokens ADD CONSTRAINT oauth1_access_tokens_user_fkey FOREIGN KEY ("user") REFERENCES user1(id) ON DELETE CASCADE;

--
-- Name: oauth1_nonce;
--

CREATE TABLE oauth1_nonce (
    id integer NOT NULL,
    consumer_key character varying,
    token character varying,
    "timestamp" integer,
    nonce character varying
);

CREATE SEQUENCE oauth1_nonce_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE oauth1_nonce_id_seq OWNED BY oauth1_nonce.id;
ALTER TABLE ONLY oauth1_nonce ALTER COLUMN id SET DEFAULT nextval('oauth1_nonce_id_seq'::regclass);
ALTER TABLE ONLY oauth1_nonce ADD CONSTRAINT oauth1_nonce_pkey PRIMARY KEY (id);

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
