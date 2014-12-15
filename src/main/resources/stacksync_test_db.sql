
DROP TABLE IF EXISTS public.item_version_chunk, public.item_version, public.item, item, public.workspace_user, public.workspace, public.device, public.user1, public.curve, public.attribute, public.abe_component, public.access_component CASCADE;
DROP SEQUENCE IF EXISTS public.sequencer_user, public.sequencer_workspace, public.sequencer_device, public.sequencer_item, public.sequencer_item_version, public.sequencer_chunk;

--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


SET search_path = public, pg_catalog;

--
-- Name: get_chunks(bigint); Type: FUNCTION; Schema: public; Owner: stacksync_user
--

CREATE FUNCTION get_chunks(bigint, OUT result text[]) RETURNS text[]
    LANGUAGE plpgsql
    AS $_$
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
$_$;


ALTER FUNCTION public.get_chunks(bigint, OUT result text[]) OWNER TO stacksync_user;

--
-- Name: get_path(bigint); Type: FUNCTION; Schema: public; Owner: stacksync_user
--

CREATE FUNCTION get_path(bigint, OUT result text) RETURNS text
    LANGUAGE plpgsql
    AS $_$
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
$_$;


ALTER FUNCTION public.get_path(bigint, OUT result text) OWNER TO stacksync_user;

--
-- Name: get_unique_chunks_to_migrate(bigint); Type: FUNCTION; Schema: public; Owner: stacksync_user
--

CREATE FUNCTION get_unique_chunks_to_migrate(bigint, OUT result text[]) RETURNS text[]
    LANGUAGE plpgsql
    AS $_$
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
$_$;


ALTER FUNCTION public.get_unique_chunks_to_migrate(bigint, OUT result text[]) OWNER TO stacksync_user;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: device; Type: TABLE; Schema: public; Owner: stacksync_user; Tablespace: 
--

CREATE TABLE device (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    name character varying(100) NOT NULL,
    user_id uuid,
    os character varying(100),
    created_at timestamp without time zone DEFAULT now(),
    last_access_at timestamp without time zone DEFAULT now(),
    last_ip inet,
    app_version character varying(45)
);


ALTER TABLE public.device OWNER TO stacksync_user;

--
-- Name: item; Type: TABLE; Schema: public; Owner: stacksync_user; Tablespace: 
--

CREATE TABLE item (
    id bigint NOT NULL,
    workspace_id uuid NOT NULL,
    latest_version bigint NOT NULL,
    parent_id bigint,
    encrypted_dek varchar(500),
    filename character varying(100) NOT NULL,
    mimetype character varying(45) NOT NULL,
    is_folder boolean NOT NULL,
    client_parent_file_version bigint
);


ALTER TABLE public.item OWNER TO stacksync_user;

--
-- Name: item_version; Type: TABLE; Schema: public; Owner: stacksync_user; Tablespace: 
--

CREATE TABLE item_version (
    id bigint NOT NULL,
    item_id bigint NOT NULL,
    device_id uuid NOT NULL,
    version integer NOT NULL,
    committed_at timestamp without time zone,
    checksum bigint NOT NULL,
    modified_at timestamp without time zone,
    status character varying(10) NOT NULL,
    size bigint NOT NULL
);


ALTER TABLE public.item_version OWNER TO stacksync_user;

--
-- Name: item_version_chunk; Type: TABLE; Schema: public; Owner: stacksync_user; Tablespace: 
--

CREATE TABLE item_version_chunk (
    item_version_id bigint NOT NULL,
    client_chunk_name character varying(40) NOT NULL,
    chunk_order integer NOT NULL
);


ALTER TABLE public.item_version_chunk OWNER TO stacksync_user;

--
-- TABLE: curve
--
CREATE TABLE public.curve (
    id uuid NOT NULL default uuid_generate_v4(),
    type varchar(20) NOT NULL,
    q varchar(200) NOT NULL,
    r varchar(200) NOT NULL,
    h varchar(200) NOT NULL,
    exp1 integer NOT NULL,
    exp2 integer NOT NULL,
    sign0 integer NOT NULL,
    sign1 integer NOT NULL
);
-- 
-- ALTER TABLE public.curve ADD CONSTRAINT pk_curve PRIMARY KEY (id);

--
-- TABLE: attribute
--
CREATE TABLE public.attribute (
    id uuid NOT NULL default uuid_generate_v4(),
    name varchar(100) NOT NULL,
    latest_version bigint NOT NULL,
    public_key_component varchar(500) NOT NULL,
    history_list json
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
    id uuid NOT NULL default uuid_generate_v4(),
    item_id bigint NOT NULL,
    attribute uuid NOT NULL,
    encrypted_pk_component varchar(500) NOT NULL,
    version integer NOT NULL
);

ALTER TABLE public.abe_component ADD CONSTRAINT pk_abe_component PRIMARY KEY (id);
ALTER TABLE public.abe_component ADD CONSTRAINT fk2_abe_component FOREIGN KEY (attribute) REFERENCES public.attribute (id) ON DELETE CASCADE;

--
-- Name: sequencer_item; Type: SEQUENCE; Schema: public; Owner: stacksync_user
--

CREATE SEQUENCE sequencer_item
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sequencer_item OWNER TO stacksync_user;

--
-- Name: sequencer_item; Type: SEQUENCE OWNED BY; Schema: public; Owner: stacksync_user
--

ALTER SEQUENCE sequencer_item OWNED BY item.id;


--
-- Name: sequencer_item_version; Type: SEQUENCE; Schema: public; Owner: stacksync_user
--

CREATE SEQUENCE sequencer_item_version
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.sequencer_item_version OWNER TO stacksync_user;

--
-- Name: sequencer_item_version; Type: SEQUENCE OWNED BY; Schema: public; Owner: stacksync_user
--

ALTER SEQUENCE sequencer_item_version OWNED BY item_version.id;


--
-- Name: user1; Type: TABLE; Schema: public; Owner: stacksync_user; Tablespace: 
--

CREATE TABLE user1 (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    name character varying(100) NOT NULL,
    swift_user character varying(100) NOT NULL,
    swift_account character varying(100) NOT NULL,
    email character varying(100) NOT NULL,
    quota_limit integer NOT NULL,
    quota_used integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.user1 OWNER TO stacksync_user;

--
-- Name: workspace; Type: TABLE; Schema: public; Owner: stacksync_user; Tablespace: 
--

CREATE TABLE workspace (
    id uuid DEFAULT uuid_generate_v4() NOT NULL,
    latest_revision character varying(45) DEFAULT 0 NOT NULL,
    owner_id uuid NOT NULL,
    is_shared boolean NOT NULL,
    is_encrypted boolean DEFAULT false NOT NULL,
    is_abe_encrypted boolean NOT NULL DEFAULT false,
    swift_container character varying(45),
    swift_url character varying(250),
    created_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.workspace OWNER TO stacksync_user;

--
-- Name: workspace_user; Type: TABLE; Schema: public; Owner: stacksync_user; Tablespace: 
--

CREATE TABLE workspace_user (
    workspace_id uuid NOT NULL,
    user_id uuid NOT NULL,
    workspace_name character varying(255) NOT NULL,
    parent_item_id bigint,
    created_at timestamp without time zone DEFAULT now(),
    modified_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.workspace_user OWNER TO stacksync_user;

--
-- Name: id; Type: DEFAULT; Schema: public; Owner: stacksync_user
--

ALTER TABLE ONLY item ALTER COLUMN id SET DEFAULT nextval('sequencer_item'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: stacksync_user
--

ALTER TABLE ONLY item_version ALTER COLUMN id SET DEFAULT nextval('sequencer_item_version'::regclass);


--
-- Data for Name: device; Type: TABLE DATA; Schema: public; Owner: stacksync_user
--

COPY device (id, name, user_id, os, created_at, last_access_at, last_ip, app_version) FROM stdin;
00000000-0000-0001-0000-000000000001	API	\N	\N	2014-05-27 14:12:51.223245	2014-05-27 14:12:51.223245	\N	\N
66fe641e-28cb-4080-af96-e5dc92a2a4d6	Cotes_PC	19d14341-e6a1-4850-8b2c-0e09869629ee	Windows-amd64	2014-06-01 21:31:57.246895	2014-06-01 21:31:57.246895	\N	\N
6834bd0d-6543-41f6-a79e-b2042a206463	Cotes_PC	19d14341-e6a1-4850-8b2c-0e09869629ee	Windows-amd64	2014-06-01 21:36:37.213804	2014-06-01 21:36:37.213804	\N	\N
d6d7f7d1-05af-462d-bb43-ebc5cf3f7afc	Cotes_PC	19d14341-e6a1-4850-8b2c-0e09869629ee	Windows-amd64	2014-06-01 21:43:58.920033	2014-06-01 21:43:58.920033	\N	\N
a3e7019e-d459-497c-b29a-d15a9edcacb5	Cotes_PC	19d14341-e6a1-4850-8b2c-0e09869629ee	Windows-amd64	2014-06-01 21:44:38.238289	2014-06-01 21:44:38.238289	\N	\N
8b66dc7f-e985-4c1b-9083-b67a06864eb7	Cotes_PC	19d14341-e6a1-4850-8b2c-0e09869629ee	Windows-amd64	2014-06-01 21:47:00.11423	2014-06-01 21:47:00.11423	\N	\N
ec177ae0-c500-4bf6-a296-107970ac4499	Cotes_PC	19d14341-e6a1-4850-8b2c-0e09869629ee	Windows-amd64	2014-06-01 21:48:36.788763	2014-06-01 21:48:36.788763	\N	\N
29e82e44-c197-472c-90aa-402689ba9fb0	Cotes_PC	19d14341-e6a1-4850-8b2c-0e09869629ee	Windows-amd64	2014-06-01 21:56:25.929846	2014-06-01 21:56:25.929846	\N	\N
fdc42307-011a-4af9-adea-8bc3aba60d07	Cotes_PC	19d14341-e6a1-4850-8b2c-0e09869629ee	Windows-amd64	2014-06-01 21:57:57.027915	2014-06-01 21:57:57.027915	\N	\N
82aa9b4a-16d2-4cd0-b06c-c86eae72632b	Cotes_PC	19d14341-e6a1-4850-8b2c-0e09869629ee	Windows-amd64	2014-06-01 22:00:03.105533	2014-06-01 22:00:03.105533	\N	\N
889efbec-2b5b-4da9-82ac-b191a967c7ba	Cotes_PC	19d14341-e6a1-4850-8b2c-0e09869629ee	Windows-amd64	2014-06-01 22:02:34.701809	2014-06-01 22:02:34.701809	\N	\N
37a72e27-ad7b-4f87-a173-0c8cf3d5d48b	Cotes_PC	19d14341-e6a1-4850-8b2c-0e09869629ee	Windows-amd64	2014-06-01 22:05:56.549547	2014-06-01 22:05:56.549547	\N	\N
f9e9b1df-1db8-4554-a1d4-dd96b39df017	Cotes_PC	19d14341-e6a1-4850-8b2c-0e09869629ee	Windows-amd64	2014-06-01 22:10:37.049737	2014-06-01 22:10:37.049737	\N	\N
5988f62f-3cba-4257-b29c-f316a325aa61	Cotes_PC	19d14341-e6a1-4850-8b2c-0e09869629ee	Windows-amd64	2014-06-01 22:14:36.889912	2014-06-01 22:14:36.889912	\N	\N
dba42279-3420-4ce7-b2f1-7e4bda9296fb	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-02 11:34:14.070215	2014-06-02 11:34:14.070215	\N	\N
f4bd0eb5-cb7c-4a34-b17c-3300f50353b7	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-02 14:58:23.793344	2014-06-02 14:58:23.793344	\N	\N
78b3122a-88c1-452f-8011-447bc8045aa8	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-02 14:58:58.51613	2014-06-02 14:58:58.51613	\N	\N
13b7fb1a-dcfd-44e5-a9e3-cab8c81158c2	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-02 15:18:06.62754	2014-06-02 15:18:06.62754	\N	\N
f0143445-e304-4a11-a83e-615b1f1947b2	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-02 17:31:55.125024	2014-06-02 17:31:55.125024	\N	\N
5b01b5ac-94c2-47af-a41b-ee9f3c974a78	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-02 17:38:39.777028	2014-06-02 17:38:39.777028	\N	\N
6ec7f422-4c87-4c7c-a19a-d3efe4b75034	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-02 17:40:53.931816	2014-06-02 17:40:53.931816	\N	\N
6003127e-b4c0-42a5-8b54-a61186e5a97f	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-02 17:41:29.199009	2014-06-02 17:41:29.199009	\N	\N
44099b50-3399-4c2c-a4d8-0b09ef83cc20	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-02 17:42:04.452474	2014-06-02 17:42:04.452474	\N	\N
eaa91d50-91c2-4456-8e3e-41362a96927b	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-02 17:42:46.689165	2014-06-02 17:42:46.689165	\N	\N
6bfe069f-9930-4e0b-b8dc-26c659edb152	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-02 17:44:04.021543	2014-06-02 17:44:04.021543	\N	\N
687ad74d-c266-4202-be5c-41be0ba9b717	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-02 17:44:39.6348	2014-06-02 17:44:39.6348	\N	\N
2e9aa108-0b40-407a-bd7a-266b90355ac2	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-02 17:46:15.890636	2014-06-02 17:46:15.890636	\N	\N
b671c49d-61b6-43ce-9847-a169ca618dae	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-03 10:31:58.658496	2014-06-03 10:31:58.658496	\N	\N
11c58703-ffb9-4b29-8b8d-5fafefd7bfd5	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-03 10:32:50.075579	2014-06-03 10:32:50.075579	\N	\N
5e8e71bd-5b7c-4520-96dc-bf72eeeed594	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-03 12:44:35.659815	2014-06-03 12:44:35.659815	\N	\N
f43dd6b5-8996-4678-b6a7-643249c61991	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-03 12:46:13.10276	2014-06-03 12:46:13.10276	\N	\N
f089410a-f804-4986-b354-701005902c2e	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-03 12:47:59.401108	2014-06-03 12:47:59.401108	\N	\N
fcbec883-66ae-4197-a8d0-8c8661848c4e	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-05 11:49:20.257574	2014-06-05 11:49:20.257574	\N	\N
3c05a747-6442-4c98-92ea-17b60f1a7d92	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-05 12:56:05.674349	2014-06-05 12:56:05.674349	\N	\N
407db21f-c80b-426f-9bf7-38344aad9873	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-05 14:56:56.851399	2014-06-05 14:56:56.851399	\N	\N
d9055267-d072-409b-9d99-de1dba5b84fc	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-05 14:58:21.532996	2014-06-05 14:58:21.532996	\N	\N
6f26626f-01ce-4e75-9579-69274dc5c434	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-06-05 15:03:28.504677	2014-06-05 15:03:28.504677	\N	\N
edcf42a7-62a4-41f8-bf32-0655471026c9	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-10 10:35:00.902184	2014-06-10 10:35:00.902184	\N	\N
eeddaca8-c69a-4992-a3ce-cf70903bcf48	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-10 10:53:39.846128	2014-06-10 10:53:39.846128	\N	\N
f35579d9-9d26-4680-8e66-ae4af1ea85cb	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-10 11:07:30.653556	2014-06-10 11:07:30.653556	\N	\N
236f8901-df1c-4aa9-acca-767b7daa47b4	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-10 11:09:30.331053	2014-06-10 11:09:30.331053	\N	\N
9d6308fe-a365-4c02-b6fe-6406be2e26f3	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-10 11:12:00.09994	2014-06-10 11:12:00.09994	\N	\N
b8e851d6-68cf-4b6d-8219-8cad991cb7da	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-10 11:12:33.427723	2014-06-10 11:12:33.427723	\N	\N
6c958373-dc0f-42b9-ac77-a078ff4d2898	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-10 11:14:47.23055	2014-06-10 11:14:47.23055	\N	\N
410843cb-b4a4-4ad1-93bb-099299d79a48	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-10 11:26:48.976861	2014-06-10 11:26:48.976861	\N	\N
0aa5d20c-5d16-414f-97c2-c5ef248e976d	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-10 11:41:15.652589	2014-06-10 11:41:15.652589	\N	\N
ca5e53ed-d645-42aa-aaf7-83d2d6a3a38a	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-10 12:07:50.749688	2014-06-10 12:07:50.749688	\N	\N
68580b5f-d64f-4738-ad76-6e1ff1d40f3c	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-13 16:34:08.989436	2014-06-13 16:34:08.989436	\N	\N
deb3e598-0df9-44ae-b2a6-1ef28ff88ec2	ast_lab_1	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-20 17:08:25.233012	2014-06-20 17:08:25.233012	\N	\N
e360c453-f528-49e0-b03c-8e0c5d71827a	ast_lab_1	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-23 09:07:34.1067	2014-06-23 09:07:34.1067	\N	\N
b7be802e-a8ee-40b2-9cfa-bbb2c4d0da81	ast_lapto	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Windows-amd64	2014-06-25 00:35:46.331363	2014-06-25 00:35:46.331363	\N	\N
ed63b321-6f03-4891-a98d-7d072ccadb76	ast_lapto	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Windows-i386	2014-06-25 00:45:34.632113	2014-06-25 00:45:34.632113	\N	\N
06c5f034-f640-491d-8d53-8599dd5a1faa	ast_lapto	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Windows-i386	2014-06-25 00:54:58.985676	2014-06-25 00:54:58.985676	\N	\N
28f2ed9a-f840-475b-a29f-a71fdcb41ac8	ast_lapto	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Windows-amd64	2014-06-26 09:27:50.198162	2014-06-26 09:27:50.198162	\N	\N
d1e18778-5bfc-4b4c-847c-58b208f52172	lab144_PC	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Windows-amd64	2014-06-26 09:39:05.542639	2014-06-26 09:39:05.542639	\N	\N
c65d0862-c302-46fc-87a8-795d2ea03b08	ast_lapto	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Windows-i386	2014-06-26 12:00:11.043871	2014-06-26 12:00:11.043871	\N	\N
0ed77390-a929-4c00-ab4c-81b7ba96ac20	ast_lab_1	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-06-30 09:06:04.940458	2014-06-30 09:06:04.940458	\N	\N
6c740aa3-f3a6-4c0f-bae8-21d94a257d45	ast05	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-07-02 10:15:40.704766	2014-07-02 10:15:40.704766	\N	\N
799c3793-07a5-4e57-9a74-cf27c956e385	lab144_PC	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Windows-i386	2014-07-02 11:10:00.374548	2014-07-02 11:10:00.374548	\N	\N
18705d96-77ee-4378-bd95-274191230072	lab144_PC	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Windows-i386	2014-07-02 11:12:17.363105	2014-07-02 11:12:17.363105	\N	\N
3b80b7db-d726-4d6f-9264-ada5ce9761c3	lab144_PC	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Windows-i386	2014-07-02 11:20:16.729161	2014-07-02 11:20:16.729161	\N	\N
1aa6fbee-9461-4ee6-b1df-98e375fa0f94	lab144_PC	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Windows-i386	2014-07-02 11:22:01.488882	2014-07-02 11:22:01.488882	\N	\N
27b36075-a268-45f4-a963-2952d83162da	lab144_PC	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Windows-i386	2014-07-02 11:30:48.265152	2014-07-02 11:30:48.265152	\N	\N
342b2f9c-4923-4f37-b5a7-40b333027df6	lab144_PC	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Windows-i386	2014-07-02 11:32:24.767673	2014-07-02 11:32:24.767673	\N	\N
b4d44a27-eb98-492d-a079-ea9f019bb704	Lab144_PC	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Windows-i386	2014-07-02 12:01:27.296913	2014-07-02 12:01:27.296913	\N	\N
c1d08dbe-b5db-47de-978a-1600c8a68029	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-07-14 13:04:18.780799	2014-07-14 13:04:18.780799	\N	\N
115559bf-9af7-471f-8965-432de5a864a5	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-07-14 13:14:27.856762	2014-07-14 13:14:27.856762	\N	\N
749653a2-1468-4614-83e2-f1aaeb85fa6b	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-07-14 13:16:15.492103	2014-07-14 13:16:15.492103	\N	\N
19fc2e91-864e-4e4e-8373-116beff54f60	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-07-21 09:59:11.381203	2014-07-21 09:59:11.381203	\N	\N
980ae8e3-4b5a-4d9e-a486-a4dafcf43a65	ast_lab_1	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-07-31 14:56:06.795736	2014-07-31 14:56:06.795736	\N	\N
0d1b272b-cb5d-40bc-86a3-8aee95b94e89	ast_lab_1	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-09-08 14:35:06.893476	2014-09-08 14:35:06.893476	\N	\N
9e498e4a-4521-4858-aed7-b7388af59bf2	ast_lab_1	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-09-08 14:37:21.003773	2014-09-08 14:37:21.003773	\N	\N
f34237bd-069f-496c-a5a3-702aaf6d4bc6	ast_lab_1	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-09-15 09:18:34.417389	2014-09-15 09:18:34.417389	\N	\N
b6f769e5-dcd8-4ebf-850c-8fb67a6c9799	ast_lab_1	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-09-22 09:27:00.767275	2014-09-22 09:27:00.767275	\N	\N
b2fa9871-387c-4093-8469-0447a9205c87	ast_lab_1	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-13 09:08:14.007126	2014-10-13 09:08:14.007126	\N	\N
4137157d-eacf-4861-b2df-12ca3e501b9a	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-16 15:55:18.44276	2014-10-16 15:55:18.44276	\N	\N
f59b7f62-c077-41a0-8bb6-f7cc037d4fea	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-16 15:57:49.592017	2014-10-16 15:57:49.592017	\N	\N
361daf82-ddc1-4aaa-a307-8e78f851a0aa	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-16 16:01:41.213518	2014-10-16 16:01:41.213518	\N	\N
7f56f422-f7d8-4d36-8458-3d94e9877ae7	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-17 12:00:04.518217	2014-10-17 12:00:04.518217	\N	\N
dc753703-7054-4c02-91de-b9f2ed53bef1	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-10-17 12:24:16.677157	2014-10-17 12:24:16.677157	\N	\N
6b8fc7ae-d325-4817-84f4-dbf5864af67e	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-10-17 12:25:35.827858	2014-10-17 12:25:35.827858	\N	\N
543b56b0-740b-4897-803d-c7166234961a	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-10-17 12:27:36.817442	2014-10-17 12:27:36.817442	\N	\N
f5b9d257-4339-4a66-bb7a-779cf1a09532	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 10:41:46.545679	2014-10-20 10:41:46.545679	\N	\N
8b49be27-99fe-4941-b583-8c689c4273a5	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 10:45:03.345117	2014-10-20 10:45:03.345117	\N	\N
abc69cc2-982f-40e6-8e06-79cf2672f296	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 11:22:54.65839	2014-10-20 11:22:54.65839	\N	\N
e95a16d0-3f42-4498-bb30-2c905afc3b13	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 11:27:02.392964	2014-10-20 11:27:02.392964	\N	\N
113865fa-9998-4c88-80e8-1c9952d8a4a0	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 11:36:57.652508	2014-10-20 11:36:57.652508	\N	\N
3f4207c4-fc9f-44a7-9f43-6c75c1cacc93	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 12:01:45.633888	2014-10-20 12:01:45.633888	\N	\N
118afe79-d643-4ea7-886c-36ed0ced9bb7	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 12:03:58.603182	2014-10-20 12:03:58.603182	\N	\N
d09a184c-7bd8-4a50-8f8c-1d17cc71da4b	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 10:52:00.619905	2014-10-20 10:52:00.619905	\N	\N
1ea8f563-da73-44d4-9f64-e852331fefde	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 11:20:50.294194	2014-10-20 11:20:50.294194	\N	\N
88e7c5b4-5a42-4e15-9866-37646516e214	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 11:14:01.11028	2014-10-20 11:14:01.11028	\N	\N
61661b23-b38e-45d2-a6e7-bdc3134d88c8	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 11:18:14.160958	2014-10-20 11:18:14.160958	\N	\N
b7889f4e-184f-4cea-9af9-b1eb77242a56	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 12:18:33.679244	2014-10-20 12:18:33.679244	\N	\N
f215b520-5595-4bec-9666-ebececa3f8c0	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 12:30:26.934026	2014-10-20 12:30:26.934026	\N	\N
20283f39-2b4f-49fe-83a9-e2bc64736d4b	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 12:31:38.403511	2014-10-20 12:31:38.403511	\N	\N
55e3eb61-1dd0-482e-966d-e5d08f487b42	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 12:35:55.594262	2014-10-20 12:35:55.594262	\N	\N
b74c56c8-ed25-4a60-bcc8-1e4b18c4514f	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 12:39:59.711609	2014-10-20 12:39:59.711609	\N	\N
347b721d-8e32-4e00-8e25-38ef42b98fb1	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 12:41:54.602881	2014-10-20 12:41:54.602881	\N	\N
45227ee7-5a4d-441e-aec2-d35358a0e069	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 13:03:53.138675	2014-10-20 13:03:53.138675	\N	\N
a9e43fe0-df32-4764-a44e-5a4211b0403d	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 15:01:22.791752	2014-10-20 15:01:22.791752	\N	\N
e594e53b-2a0d-4564-923d-901ee1b85a45	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 15:04:16.725948	2014-10-20 15:04:16.725948	\N	\N
724c4b4a-c5a7-4452-9700-490e142d2301	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 15:10:41.501559	2014-10-20 15:10:41.501559	\N	\N
27537a3c-e5c4-4c72-b44f-1cc8907829a5	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 15:13:07.01121	2014-10-20 15:13:07.01121	\N	\N
b2c3287e-3595-48db-81c1-5e86ea474e1b	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 16:17:41.166308	2014-10-20 16:17:41.166308	\N	\N
e00246ad-e5c6-44e3-98a9-404b7f46ec48	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 16:36:17.596419	2014-10-20 16:36:17.596419	\N	\N
a7325266-a8c9-42c7-b0de-010444277acb	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 16:38:49.090726	2014-10-20 16:38:49.090726	\N	\N
b47dfa8b-3df9-47ed-a1ab-72ebf6b842cd	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 16:46:48.550115	2014-10-20 16:46:48.550115	\N	\N
ae407405-015f-4023-a7cf-438dc46f77e3	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 16:51:04.909993	2014-10-20 16:51:04.909993	\N	\N
51917234-7c68-4277-a250-f6dad6e1c6f3	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 16:53:48.633624	2014-10-20 16:53:48.633624	\N	\N
4d1b93a8-ffa3-482b-ab54-798510236ee4	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 16:56:23.408875	2014-10-20 16:56:23.408875	\N	\N
171b94b0-d971-4efb-9bcd-33e0594ae8e3	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 16:59:55.870012	2014-10-20 16:59:55.870012	\N	\N
cda0c833-2e62-4855-9a2b-3983ab329213	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 17:26:41.856086	2014-10-20 17:26:41.856086	\N	\N
212943ab-9a5b-44a4-9f30-ac2705f1880b	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 17:41:19.697775	2014-10-20 17:41:19.697775	\N	\N
24f04eb7-fe45-463c-b61f-fa18f3d0ed34	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 17:41:33.575495	2014-10-20 17:41:33.575495	\N	\N
977808ad-f556-41b8-ba14-394328768f32	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 17:45:30.246901	2014-10-20 17:45:30.246901	\N	\N
6f94a5d0-7c41-44a7-86c7-afcb12e0473c	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 17:48:07.212268	2014-10-20 17:48:07.212268	\N	\N
71e7688c-3dce-443a-9529-a7f7484e3645	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-20 17:55:29.354911	2014-10-20 17:55:29.354911	\N	\N
178f68ea-bef3-4448-9751-629b48c6549a	ast_lapto	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Windows-amd64	2014-10-21 10:24:09.153657	2014-10-21 10:24:09.153657	\N	\N
8c55ef39-21f5-4df7-b787-33f5f1007a75	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-21 13:17:56.22822	2014-10-21 13:17:56.22822	\N	\N
e362701e-66f4-4cb9-98d1-98451e01a9fd	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-21 15:15:05.875313	2014-10-21 15:15:05.875313	\N	\N
978aa637-d125-499f-9d2c-fca1ce320d23	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-21 15:29:31.329488	2014-10-21 15:29:31.329488	\N	\N
391e279f-5b32-4741-91de-3990fbad03af	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-21 15:36:24.81192	2014-10-21 15:36:24.81192	\N	\N
e07f6bcd-6668-4ebe-a7d1-ca422a3af6fe	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-22 09:59:26.293174	2014-10-22 09:59:26.293174	\N	\N
1a11be43-edab-4c6c-a44f-5fd44915cfe9	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-22 10:14:05.677022	2014-10-22 10:14:05.677022	\N	\N
0a3e95df-524f-444a-b6a5-3f40392235fe	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-22 10:24:58.874678	2014-10-22 10:24:58.874678	\N	\N
2a1fbf3a-359d-4220-8ea6-a2d99fccf46a	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-22 10:44:42.411113	2014-10-22 10:44:42.411113	\N	\N
6dba4495-d9f7-44c3-aae7-8ef404bf5daa	ast_lapto	dedfa1ae-eb90-4886-b33c-00b0529482f1	Windows-amd64	2014-10-22 11:37:59.630408	2014-10-22 11:37:59.630408	\N	\N
49b7671a-ad66-4ba7-8626-0208fd0275e6	ast_lapto	dedfa1ae-eb90-4886-b33c-00b0529482f1	Windows-amd64	2014-10-22 11:43:53.830673	2014-10-22 11:43:53.830673	\N	\N
30f81536-bb2b-4893-baee-4aa1656eeecd	ast_lapto	dedfa1ae-eb90-4886-b33c-00b0529482f1	Windows-amd64	2014-10-22 12:19:52.616623	2014-10-22 12:19:52.616623	\N	\N
16b83cef-98d5-4527-a871-8d2e6b4016d8	ast_lapto	dedfa1ae-eb90-4886-b33c-00b0529482f1	Windows-amd64	2014-10-22 12:30:59.474612	2014-10-22 12:30:59.474612	\N	\N
ca9238ba-42ed-4175-9e2c-e06a4a857634	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-22 12:31:30.013453	2014-10-22 12:31:30.013453	\N	\N
e0fbefc9-8d3e-40d3-96fb-b12c017fc348	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-22 12:39:08.935379	2014-10-22 12:39:08.935379	\N	\N
d114b77c-0221-4b46-913d-0baae1c7f987	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-22 12:42:21.229223	2014-10-22 12:42:21.229223	\N	\N
c43727a7-6f73-4715-83a6-d5eb8020bdc0	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-22 12:44:26.254587	2014-10-22 12:44:26.254587	\N	\N
fe94c19a-0e5a-437d-9055-281a1f76413f	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-22 12:47:41.606374	2014-10-22 12:47:41.606374	\N	\N
1303d0e3-17f7-4d24-8ab1-c02de4dd4f5f	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-22 12:57:44.158476	2014-10-22 12:57:44.158476	\N	\N
a7c94c3f-dfdf-4046-8ee4-9e2c240ce24d	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-22 14:41:00.271138	2014-10-22 14:41:00.271138	\N	\N
bd1c6f93-2fb3-404d-80d2-abe943967ade	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-22 14:42:30.625702	2014-10-22 14:42:30.625702	\N	\N
97d89041-3016-467c-96d4-1d9e49ddf85e	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-23 10:26:42.244714	2014-10-23 10:26:42.244714	\N	\N
10864640-f1a9-4f88-80de-b8283153c66b	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-23 10:26:42.244714	2014-10-23 10:26:42.244714	\N	\N
b016f96d-7668-4dd8-a26b-492098f94b73	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-23 10:26:54.711555	2014-10-23 10:26:54.711555	\N	\N
223a7d6b-9349-4c2d-a032-11068d502ef3	ast_lapto	dedfa1ae-eb90-4886-b33c-00b0529482f1	Windows-amd64	2014-10-23 10:44:11.801533	2014-10-23 10:44:11.801533	\N	\N
746031af-a5e7-4f2f-9620-f05dae15d804	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-23 10:45:52.211115	2014-10-23 10:45:52.211115	\N	\N
9ffa5c77-f710-4550-892a-df864a5f6364	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-23 11:45:43.309783	2014-10-23 11:45:43.309783	\N	\N
8e7c6f7d-1507-4d6c-904f-ea9ebd7dc0af	ast_lapto	dedfa1ae-eb90-4886-b33c-00b0529482f1	Windows-amd64	2014-10-23 11:46:47.341301	2014-10-23 11:46:47.341301	\N	\N
471f7d8a-ea64-4689-bb12-d0efc4298c14	ast_lapto	dedfa1ae-eb90-4886-b33c-00b0529482f1	Windows-amd64	2014-10-23 11:59:04.830609	2014-10-23 11:59:04.830609	\N	\N
57461b6f-21c1-4ac9-a7d2-dc5c17de5624	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-23 11:59:20.209154	2014-10-23 11:59:20.209154	\N	\N
56a61d9c-4cfd-4727-94eb-e7df564bbedc	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-23 12:45:02.409885	2014-10-23 12:45:02.409885	\N	\N
cac1a5c8-8cde-477c-8ea0-7b557ee0cd79	ast_lapto	dedfa1ae-eb90-4886-b33c-00b0529482f1	Windows-amd64	2014-10-23 12:46:53.468972	2014-10-23 12:46:53.468972	\N	\N
80a7cf24-874d-40eb-bf2d-35752a465768	ast_lapto	dedfa1ae-eb90-4886-b33c-00b0529482f1	Windows-amd64	2014-10-23 12:56:56.160456	2014-10-23 12:56:56.160456	\N	\N
15f18602-901c-4c66-92ac-e8aa8bd217a9	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-23 13:03:16.302162	2014-10-23 13:03:16.302162	\N	\N
547edc26-54e8-4842-933c-b4d08538ee47	ast_cotes	7323d7b0-9d6e-488b-801d-58bd4bd687f9	Linux-amd64	2014-10-23 16:40:06.876516	2014-10-23 16:40:06.876516	\N	\N
fc234060-524f-4ed2-a002-59442ae1e9f1	ast_cotes	7323d7b0-9d6e-488b-801d-58bd4bd687f9	Linux-amd64	2014-10-23 17:08:25.376783	2014-10-23 17:08:25.376783	\N	\N
d6b8332d-166a-4658-9bf8-e6a2a3cc4d14	ast_lapto	a6993e3a-6273-4af9-9181-c7208f65d307	Windows-amd64	2014-10-23 17:15:27.722337	2014-10-23 17:15:27.722337	\N	\N
40536ea8-2f6c-4323-a641-252a011bfca5	ast_cotes	7323d7b0-9d6e-488b-801d-58bd4bd687f9	Linux-amd64	2014-10-24 16:05:40.642019	2014-10-24 16:05:40.642019	\N	\N
d1722be3-cc52-4f40-b8dd-634573d87306	ast_cotes	7323d7b0-9d6e-488b-801d-58bd4bd687f9	Linux-amd64	2014-10-24 16:13:17.00844	2014-10-24 16:13:17.00844	\N	\N
e4c2ddcc-790d-4240-b481-d660a27bd021	lab144_PC	a6993e3a-6273-4af9-9181-c7208f65d307	Windows-i386	2014-10-27 10:50:29.765589	2014-10-27 10:50:29.765589	\N	\N
5e052b78-2d68-4395-90d5-e804abfbd05c	ast_cotes	7323d7b0-9d6e-488b-801d-58bd4bd687f9	Linux-amd64	2014-10-27 10:51:31.582388	2014-10-27 10:51:31.582388	\N	\N
300c21f8-6c02-415e-8bf4-d9348cb075ca	lab144_PC	a6993e3a-6273-4af9-9181-c7208f65d307	Windows-i386	2014-10-27 10:59:02.659754	2014-10-27 10:59:02.659754	\N	\N
18364095-66f9-49d6-a572-db9bb96c7ecd	lab144_PC	a6993e3a-6273-4af9-9181-c7208f65d307	Windows-i386	2014-10-27 11:41:27.83533	2014-10-27 11:41:27.83533	\N	\N
b2fc3c15-cf50-4d91-b703-d288433320ad	lab144_PC	a6993e3a-6273-4af9-9181-c7208f65d307	Windows-i386	2014-10-27 11:42:59.336927	2014-10-27 11:42:59.336927	\N	\N
ee13a071-43e7-45c7-9f36-710ef347d909	lab144_PC	a6993e3a-6273-4af9-9181-c7208f65d307	Windows-i386	2014-10-27 12:27:26.104166	2014-10-27 12:27:26.104166	\N	\N
fb089c0c-5208-4600-967c-7b274a3c814c	ast_cotes	7323d7b0-9d6e-488b-801d-58bd4bd687f9	Linux-amd64	2014-10-27 12:28:14.901322	2014-10-27 12:28:14.901322	\N	\N
2faca814-add1-4326-8407-54675b50464d	ast_cotes	7323d7b0-9d6e-488b-801d-58bd4bd687f9	Linux-amd64	2014-10-27 15:14:05.609349	2014-10-27 15:14:05.609349	\N	\N
a3cc929c-37f4-4f5b-b822-e34617931250	ast_cotes	7323d7b0-9d6e-488b-801d-58bd4bd687f9	Linux-amd64	2014-10-27 15:18:46.084043	2014-10-27 15:18:46.084043	\N	\N
6524f05a-bdfc-499f-8371-05e2664ebd2a	ast_cotes	7323d7b0-9d6e-488b-801d-58bd4bd687f9	Linux-amd64	2014-10-27 15:20:17.230259	2014-10-27 15:20:17.230259	\N	\N
64aa6d37-615e-4381-8aa9-6ffd1cd47267	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-10-29 12:27:37.43465	2014-10-29 12:27:37.43465	\N	\N
db034907-be61-4f4b-8a64-f3c009836310	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-10-29 12:54:19.356106	2014-10-29 12:54:19.356106	\N	\N
2e08aac2-dab8-4c4d-aa60-5b8116a9daa4	ast_cotes	19d14341-e6a1-4850-8b2c-0e09869629ee	Linux-amd64	2014-10-29 14:32:53.899871	2014-10-29 14:32:53.899871	\N	\N
fdc40bd9-0c0c-4a0c-8e33-ff3b4d108764	ast_cotes	dedfa1ae-eb90-4886-b33c-00b0529482f1	Linux-amd64	2014-10-29 14:47:17.04751	2014-10-29 14:47:17.04751	\N	\N
628787e5-f1e8-4308-b64a-e249b9bf805d	ast_lab_1	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-10-31 12:54:08.126254	2014-10-31 12:54:08.126254	\N	\N
81687eb1-1d4d-49ba-a6b3-596b38f33ba3	ast_cotes	09c58a46-a793-40f9-83b9-ee22d07a038f	Linux-amd64	2014-11-03 09:50:54.920225	2014-11-03 09:50:54.920225	\N	\N
d3b26e89-fdda-4e76-9822-866790d26e62	ast_cotes	09c58a46-a793-40f9-83b9-ee22d07a038f	Linux-amd64	2014-11-03 11:02:12.178983	2014-11-03 11:02:12.178983	\N	\N
c182f272-83ae-473b-852d-acf5f1b65c6c	ast_cotes	56ffff9b-a18d-4638-a9c3-a542c85e52bf	Linux-amd64	2014-11-03 15:17:49.76985	2014-11-03 15:17:49.76985	\N	\N
915a7782-71b3-44ba-ac47-222f2965d7bc	ast_cotes	a6993e3a-6273-4af9-9181-c7208f65d307	Linux-amd64	2014-11-05 16:00:08.520284	2014-11-05 16:00:08.520284	\N	\N
41a5cfdf-9d49-4c1c-b55f-1d47b729eb65	ast_cotes	a6993e3a-6273-4af9-9181-c7208f65d307	Linux-amd64	2014-11-05 17:21:08.727306	2014-11-05 17:21:08.727306	\N	\N
\.

COPY item (id, workspace_id, latest_version, parent_id, filename, mimetype, is_folder, client_parent_file_version) FROM stdin;
49	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Megaptera.jpg	image/jpeg	f	\N
5	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Untitled Folder	unknown	t	\N
6	d9c10e45-002b-4240-804a-3b4e24497e5c	1	\N	demo	unknown	t	\N
7	d9c10e45-002b-4240-804a-3b4e24497e5c	1	6	documents	unknown	t	1
8	d9c10e45-002b-4240-804a-3b4e24497e5c	1	6	images	unknown	t	1
9	d9c10e45-002b-4240-804a-3b4e24497e5c	1	7	lorem.pdf	application/pdf	f	1
71	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	15	116	informacion	inode/directory	t	1
39	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Megaptera.jpg	image/jpeg	f	\N
40	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Megaptera.jpg	image/jpeg	f	\N
41	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Megaptera.jpg	image/jpeg	f	\N
45	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Megaptera.jpg	image/jpeg	f	\N
81	a0d404a4-7240-4750-89b2-3c01601b0c1d	5	54	null	image/jpeg	f	1
33	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	New Folder 1	inode/directory	t	\N
53	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Images	inode/directory	t	\N
30	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	New File 5.txt	text/plain	f	\N
24	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	New File 1.txt	text/plain	f	\N
25	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	New File 2.txt	text/plain	f	\N
26	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	New File 3.txt	text/plain	f	\N
28	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	New File 4.txt	text/plain	f	\N
15	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	New File.txt	text/plain	f	\N
16	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	sss.txt	text/plain	f	\N
32	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	a	inode/directory	t	\N
34	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	32	New Folder	inode/directory	t	1
31	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	New Folder	inode/directory	t	\N
54	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	1	Images 1	inode/directory	t	1
36	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	800px-Sasso_lungo_da_passo_pordoi	image/jpeg	f	\N
37	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	images.jpg	image/jpeg	f	\N
38	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Megaptera.jpg	image/jpeg	f	\N
46	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Megaptera.jpg	image/jpeg	f	\N
47	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Megaptera.jpg	image/jpeg	f	\N
48	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Megaptera.jpg	image/jpeg	f	\N
80	a0d404a4-7240-4750-89b2-3c01601b0c1d	7	1	Megaptera_6.jpg	image/jpeg	f	1
20	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	1	a.txt	text/plain	f	1
21	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	1	b.txt	text/plain	f	1
35	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	1	New Folder	inode/directory	t	1
18	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	1	New File 1.txt	text/plain	f	1
19	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	1	New File 2.txt	text/plain	f	1
22	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	1	New File 3.txt	text/plain	f	1
23	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	1	New File 4.txt	text/plain	f	1
27	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	1	New File 5.txt	text/plain	f	1
29	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	1	New File 6.txt	text/plain	f	1
17	a0d404a4-7240-4750-89b2-3c01601b0c1d	13	1	New File.txt	text/plain	f	1
42	a0d404a4-7240-4750-89b2-3c01601b0c1d	4	1	New File.txt	text/plain	f	1
50	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	New File.txt	text/plain	f	\N
51	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	New Folder	inode/directory	t	\N
65	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	1	New File 1.txt	text/plain	f	1
72	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	1	New File.txt	text/plain	f	1
73	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	1	New File.txt	text/plain	f	1
66	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	1	New Folder 1 1	inode/directory	t	1
67	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	66	a.txt	text/plain	f	1
68	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	66	Images	inode/directory	t	1
69	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	68	Megaptera.jpg	image/jpeg	f	1
70	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	68	Slovakia.jpg	image/jpeg	f	1
60	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	1	New Folder 1	inode/directory	t	1
61	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	60	Images	inode/directory	t	1
62	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	61	Slovakia.jpg	image/jpeg	f	1
63	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	61	Megaptera.jpg	image/jpeg	f	1
64	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	60	a.txt	text/plain	f	1
74	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	New Folder 1	inode/directory	t	\N
2	a0d404a4-7240-4750-89b2-3c01601b0c1d	4	1	new_file.txt	text/plain	f	1
10	d9c10e45-002b-4240-804a-3b4e24497e5c	3	8	estampa-nevada (ast_cotes' conflicting copy, 2013-07-10 17-41-20).jpg	image/jpeg	f	1
13	d9c10e45-002b-4240-804a-3b4e24497e5c	3	8	arboles (ast_cotes' conflicting copy, 2013-07-10 17-42-14).jpg	image/jpeg	f	1
55	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	54	Slovakia.jpg	image/jpeg	f	1
56	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	54	Megaptera.jpg	image/jpeg	f	1
1	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	\N	prueba	inode/directory	t	\N
3	a0d404a4-7240-4750-89b2-3c01601b0c1d	4	1	new_file2.txt	text/plain	f	1
43	a0d404a4-7240-4750-89b2-3c01601b0c1d	6	1	prueba	inode/directory	t	1
44	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	43	a.txt	text/plain	f	1
57	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	43	Images	inode/directory	t	1
58	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	57	Megaptera.jpg	image/jpeg	f	1
59	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	57	Slovakia.jpg	image/jpeg	f	1
178	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	t	inode/directory	t	\N
79	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Megaptera 5.jpg	image/jpeg	f	\N
111	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	109	Megaptera.jpg	image/jpeg	f	1
86	d9c10e45-002b-4240-804a-3b4e24497e5c	1	8	estampa-nevada.jpg	image/jpeg	f	1
87	d9c10e45-002b-4240-804a-3b4e24497e5c	1	8	paisajes-nevados-1.jpg	image/jpeg	f	1
88	d9c10e45-002b-4240-804a-3b4e24497e5c	1	8	paisajes-playas-palmeras.jpg	image/jpeg	f	1
89	d9c10e45-002b-4240-804a-3b4e24497e5c	1	8	arboles.jpg	image/jpeg	f	1
90	d9c10e45-002b-4240-804a-3b4e24497e5c	2	8	arboles-nevados (ast_cotes' conflicting copy, 2013-07-10 17-41-34).jpg	image/jpeg	f	1
12	d9c10e45-002b-4240-804a-3b4e24497e5c	3	8	paisajes-playas-palmeras (ast_cotes' conflicting copy, 2013-07-10 17-42-00).jpg	image/jpeg	f	1
11	d9c10e45-002b-4240-804a-3b4e24497e5c	3	8	paisajes-nevados-1 (ast_cotes' conflicting copy, 2013-07-10 17-41-43).jpg	image/jpeg	f	1
14	d9c10e45-002b-4240-804a-3b4e24497e5c	3	8	arboles-nevados.jpg	image/jpeg	f	1
112	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Images	inode/directory	t	\N
113	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	112	Slovakia.jpg	image/jpeg	f	1
123	a0d404a4-7240-4750-89b2-3c01601b0c1d	4	\N	Images 1	inode/directory	t	\N
92	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Megaptera 5.jpg	image/jpeg	f	\N
85	a0d404a4-7240-4750-89b2-3c01601b0c1d	4	\N	Megaptera 1.jpg	image/jpeg	f	\N
116	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	1	\N	Images	inode/directory	t	\N
96	a0d404a4-7240-4750-89b2-3c01601b0c1d	1	95	Megaptera.jpg	image/jpeg	f	1
97	a0d404a4-7240-4750-89b2-3c01601b0c1d	1	95	Slovakia.jpg	image/jpeg	f	1
151	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	116	gimp_3.png	image/png	f	1
95	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	95	Images	inode/directory	t	2
100	a0d404a4-7240-4750-89b2-3c01601b0c1d	1	99	Slovakia.jpg	image/jpeg	f	1
101	a0d404a4-7240-4750-89b2-3c01601b0c1d	1	99	Megaptera.jpg	image/jpeg	f	1
78	a0d404a4-7240-4750-89b2-3c01601b0c1d	8	126	Megaptera_4.jpg	image/jpeg	f	1
103	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	102	Slovakia.jpg	image/jpeg	f	1
104	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	102	Megaptera.jpg	image/jpeg	f	1
52	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	126	Megaptera.jpg	image/jpeg	f	1
114	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	112	Megaptera.jpg	image/jpeg	f	1
99	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	99	Images	inode/directory	t	1
102	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	1	Images	inode/directory	t	1
83	a0d404a4-7240-4750-89b2-3c01601b0c1d	5	43	prueba.txt	text/plain	f	2
84	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	43	informacion.txt	text/plain	f	3
106	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	105	Slovakia.jpg	image/jpeg	f	1
107	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	105	Megaptera.jpg	image/jpeg	f	1
82	a0d404a4-7240-4750-89b2-3c01601b0c1d	4	1	New File.txt	text/plain	f	1
109	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Images	inode/directory	t	\N
110	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	109	Slovakia.jpg	image/jpeg	f	1
93	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	\N	sebas.txt	text/plain	f	\N
105	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	43	Images 1	inode/directory	t	1
115	a0d404a4-7240-4750-89b2-3c01601b0c1d	4	126	sebas.txt	text/plain	f	1
127	a0d404a4-7240-4750-89b2-3c01601b0c1d	4	126	Dialog.txt	text/plain	f	1
124	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	123	Megaptera.jpg	image/jpeg	f	1
125	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	123	Slovakia.jpg	image/jpeg	f	1
121	a0d404a4-7240-4750-89b2-3c01601b0c1d	9	\N	Dialog 1.txt	text/plain	f	\N
144	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	126	informacion	inode/directory	t	2
94	a0d404a4-7240-4750-89b2-3c01601b0c1d	4	\N	Megaptera 2 1.jpg	image/jpeg	f	\N
122	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Megaptera 3.jpg	image/jpeg	f	\N
131	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	126	FileA.txt	text/plain	f	2
132	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	126	A1	inode/directory	t	2
135	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	126	informacion	inode/directory	t	2
136	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	135	Images	inode/directory	t	1
137	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	136	Megaptera.jpg	image/jpeg	f	1
138	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	136	Slovakia.jpg	image/jpeg	f	1
139	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	135	Megaptera 2 1 1.jpg	image/jpeg	f	1
140	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	135	Megaptera 3.jpg	image/jpeg	f	1
141	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	135	Megaptera 2.jpg	image/jpeg	f	1
134	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	126	Megaptera 3.jpg	image/jpeg	f	2
133	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	126	New Document.edoc	application/zip	f	2
119	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	\N	New Document.edoc	application/zip	f	\N
142	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	126	New Document.edoc	application/zip	f	2
143	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	126	Megaptera 3.jpg	image/jpeg	f	2
148	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	144	Megaptera 2 1 1.jpg	image/jpeg	f	1
150	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	144	Megaptera 2.jpg	image/jpeg	f	1
149	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	144	Megaptera 3.jpg	image/jpeg	f	1
126	a0d404a4-7240-4750-89b2-3c01601b0c1d	5	\N	A	inode/directory	t	\N
145	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	144	Images	inode/directory	t	1
146	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	145	Megaptera.jpg	image/jpeg	f	1
147	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	145	Slovakia.jpg	image/jpeg	f	1
153	a0d404a4-7240-4750-89b2-3c01601b0c1d	1	\N	gimp.png	image/png	f	\N
152	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	\N	Wellcome.txt	text/plain	f	\N
154	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	126	B	inode/directory	t	2
181	d9c10e45-002b-4240-804a-3b4e24497e5c	1	171	yyyyo	inode/directory	t	1
182	d9c10e45-002b-4240-804a-3b4e24497e5c	1	171	stacksync-android-latest-1.apk	application/jar	f	1
161	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	ueventd.rc	text/plain	f	\N
164	d9c10e45-002b-4240-804a-3b4e24497e5c	1	6	default.prop	text/plain	f	1
165	d9c10e45-002b-4240-804a-3b4e24497e5c	1	\N	1024x600_03.jpg	image/jpeg	f	\N
166	d9c10e45-002b-4240-804a-3b4e24497e5c	1	6	stacksync-android-latest-2.apk	application/jar	f	1
167	d9c10e45-002b-4240-804a-3b4e24497e5c	2	6	1024x600_05.jpg	image/jpeg	f	1
183	d9c10e45-002b-4240-804a-3b4e24497e5c	1	171	1024X600_02.jpg	image/jpeg	f	1
175	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	3	71	zzzz	inode/directory	t	3
170	d9c10e45-002b-4240-804a-3b4e24497e5c	6	\N	test_renamed	text/plain	f	\N
120	a0d404a4-7240-4750-89b2-3c01601b0c1d	5	126	bienvenido.txt	text/plain	f	1
176	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	126	Megaptera 2.jpg	image/jpeg	f	1
169	d9c10e45-002b-4240-804a-3b4e24497e5c	1	\N	cloudspaces-logo-square-big.png	image/png	f	\N
177	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	126	Megaptera 2 1 1.jpg	image/jpeg	f	1
155	a0d404a4-7240-4750-89b2-3c01601b0c1d	6	\N	welcome.txt	text/plain	f	\N
162	d9c10e45-002b-4240-804a-3b4e24497e5c	5	\N	default.prop	text/plain	f	\N
163	d9c10e45-002b-4240-804a-3b4e24497e5c	4	160	default.prop	text/plain	f	3
160	d9c10e45-002b-4240-804a-3b4e24497e5c	4	\N	fghjl_as	inode/directory	t	\N
171	d9c10e45-002b-4240-804a-3b4e24497e5c	1	\N	test	inode/directory	t	\N
173	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	IMG_20140305_183610	image/jpeg	f	\N
174	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	medstream_1_individuales_11-11.jpg	image/jpeg	f	\N
180	d9c10e45-002b-4240-804a-3b4e24497e5c	3	\N	new2.txt	application/jar	f	\N
156	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	\N	informacion 1	inode/directory	t	\N
157	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	156	Images	inode/directory	t	1
158	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	157	Slovakia.jpg	image/jpeg	f	1
159	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	157	Megaptera.jpg	image/jpeg	f	1
172	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	hola.txt	text/plain	f	\N
168	d9c10e45-002b-4240-804a-3b4e24497e5c	5	\N	folderrrr5	inode/directory	t	\N
433	c6f1148d-9e0f-426a-b48c-8aef0ebb818d	2	\N	share1	unknown	t	\N
217	d9c10e45-002b-4240-804a-3b4e24497e5c	6	\N	New2.txt	text/plain	f	\N
438	244c075f-b71f-4424-a961-4b2b9890d4de	3	\N	asdf	text/plain	f	\N
441	244c075f-b71f-4424-a961-4b2b9890d4de	3	\N	adsf	text/plain	f	\N
448	21f708c5-0a3d-46ee-a695-8e360bf04554	2	\N	share3	unknown	t	\N
431	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	New2 (ast_cotes' conflicting copy, 2014-10-20 12-41-58).txt	text/plain	f	\N
432	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	cloudspaces-logo-square-big (ast_cotes' conflicting copy, 2013-03-04 10-50-08).png	image/png	f	\N
479	b4d4eb00-34d7-46c0-976f-dba69a58283f	1	471	StackSync.pdf	application/pdf	f	1
478	b4d4eb00-34d7-46c0-976f-dba69a58283f	2	471	eWave.pdf	application/pdf	f	1
215	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	1024X600_02.jpg	image/jpeg	f	\N
216	d9c10e45-002b-4240-804a-3b4e24497e5c	1	\N	1024X600_02.jpg	image/jpeg	f	\N
214	d9c10e45-002b-4240-804a-3b4e24497e5c	3	\N	New Text Document.txt	text/plain	f	\N
227	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	finger_tables1.png	image/png	f	\N
226	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	IMG_1310.JPG	image/jpeg	f	\N
228	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	IMG_1351.JPG	image/jpeg	f	\N
230	d9c10e45-002b-4240-804a-3b4e24497e5c	2	225	edgarzamora.png	image/png	f	1
231	d9c10e45-002b-4240-804a-3b4e24497e5c	2	225	pepito	inode/directory	t	1
233	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	pedrogarcia.jpg	image/jpeg	f	\N
232	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	edgarzamora.png	image/png	f	\N
225	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	folder name	inode/directory	t	\N
229	d9c10e45-002b-4240-804a-3b4e24497e5c	2	225	finger_table2.png	image/png	f	1
234	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	65786	inode/directory	t	\N
427	4fa20d25-1dd4-44de-91eb-71fc6d784e79	2	\N	s3	unknown	t	\N
430	4fa20d25-1dd4-44de-91eb-71fc6d784e79	1	427	New folder	unknown	t	2
444	c6f1148d-9e0f-426a-b48c-8aef0ebb818d	2	433	tester4.txt	text/plain	f	2
440	244c075f-b71f-4424-a961-4b2b9890d4de	3	\N	sdfsaf	text/plain	f	\N
443	244c075f-b71f-4424-a961-4b2b9890d4de	3	\N	zxcv	text/plain	f	\N
436	244c075f-b71f-4424-a961-4b2b9890d4de	2	\N	Untitled Folder	unknown	t	\N
449	21f708c5-0a3d-46ee-a695-8e360bf04554	1	448	win_integration.png	image/png	f	2
450	21f708c5-0a3d-46ee-a695-8e360bf04554	1	448	logo48.png	image/png	f	2
451	21f708c5-0a3d-46ee-a695-8e360bf04554	1	448	logo48.ico	image/x-icon	f	2
452	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	prueba1	unknown	t	\N
455	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	prueba1	unknown	t	\N
417	6f53e591-9019-4aa7-b8ae-fa63cb9f1035	2	\N	prueba1	unknown	t	\N
458	d9c10e45-002b-4240-804a-3b4e24497e5c	1	\N	%#&(24	inode/directory	t	\N
459	d9c10e45-002b-4240-804a-3b4e24497e5c	1	\N	Sygic_13.4.1_Beni666..apk	application/jar	f	\N
460	5b99e136-e6e3-42f7-8e5c-a02f41041cd7	2	\N	test	inode/directory	t	\N
461	5b99e136-e6e3-42f7-8e5c-a02f41041cd7	2	\N	test2	inode/directory	t	\N
462	85abf38c-20c1-4116-a1c1-20112d23e67b	1	\N	deliverables	inode/directory	t	\N
463	85abf38c-20c1-4116-a1c1-20112d23e67b	2	462	D2.3_Cloudspaces_Public.pdf	application/pdf	f	1
468	d9c10e45-002b-4240-804a-3b4e24497e5c	1	\N	pepito	inode/directory	t	\N
469	d9c10e45-002b-4240-804a-3b4e24497e5c	2	468	api.pdf	application/pdf	f	1
457	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	olé	inode/directory	t	\N
470	d9c10e45-002b-4240-804a-3b4e24497e5c	1	\N	eWave Leveraging Energy-Awareness for In-Line Deduplication Clusters.pdf	application/pdf	f	\N
471	b4d4eb00-34d7-46c0-976f-dba69a58283f	1	\N	Publications	inode/directory	t	\N
480	b4d4eb00-34d7-46c0-976f-dba69a58283f	1	\N	logo.png	image/png	f	\N
481	b4d4eb00-34d7-46c0-976f-dba69a58283f	2	\N	Reducing.pdf	application/pdf	f	\N
482	b4d4eb00-34d7-46c0-976f-dba69a58283f	2	\N	Reducing.pdf	application/pdf	f	\N
483	b4d4eb00-34d7-46c0-976f-dba69a58283f	1	\N	Reducing.pdf	application/pdf	f	\N
235	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	edgarzamora.png	image/png	f	\N
236	d9c10e45-002b-4240-804a-3b4e24497e5c	4	\N	test2	inode/directory	t	\N
256	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	116	informacion	inode/directory	t	1
237	a0d404a4-7240-4750-89b2-3c01601b0c1d	3	\N	prueba.txt	text/plain	f	\N
238	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
239	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	238	Images	inode/directory	t	1
240	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	239	Megaptera.jpg	image/jpeg	f	1
241	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	239	Slovakia.jpg	image/jpeg	f	1
242	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	238	Megaptera 2.jpg	image/jpeg	f	1
243	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
244	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	243	Images	inode/directory	t	1
245	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
246	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	245	Images	inode/directory	t	1
247	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
248	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	247	Images	inode/directory	t	1
249	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
250	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	249	Images	inode/directory	t	1
251	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	250	Megaptera.jpg	image/jpeg	f	1
252	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	250	Slovakia.jpg	image/jpeg	f	1
253	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	249	Megaptera 2.jpg	image/jpeg	f	1
254	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
255	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	254	Images	inode/directory	t	1
258	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
259	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	258	Images	inode/directory	t	1
260	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
261	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	260	Images	inode/directory	t	1
262	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
263	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	262	Images	inode/directory	t	1
264	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
265	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	264	Images	inode/directory	t	1
266	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
267	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	266	Images	inode/directory	t	1
268	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
269	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	268	Images	inode/directory	t	1
270	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
271	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	270	Images	inode/directory	t	1
272	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
273	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	272	Images	inode/directory	t	1
274	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	273	Megaptera.jpg	image/jpeg	f	1
275	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	273	Slovakia.jpg	image/jpeg	f	1
276	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	272	Megaptera 2.jpg	image/jpeg	f	1
179	a0d404a4-7240-4750-89b2-3c01601b0c1d	8	\N	hello.txt	text/plain	f	\N
277	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
278	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	277	Images	inode/directory	t	1
279	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
280	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	279	Images	inode/directory	t	1
281	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
282	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	281	Images	inode/directory	t	1
283	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
284	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	283	Images	inode/directory	t	1
285	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
286	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	285	Images	inode/directory	t	1
287	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
288	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	287	Images	inode/directory	t	1
289	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
290	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	289	Images	inode/directory	t	1
291	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
292	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	291	Images	inode/directory	t	1
293	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	292	Megaptera.jpg	inode/x-empty	f	1
294	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	292	Slovakia.jpg	inode/x-empty	f	1
295	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	291	Megaptera 2.jpg	inode/x-empty	f	1
296	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
297	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	296	Images	inode/directory	t	1
298	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
299	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	298	Images	inode/directory	t	1
300	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
301	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	300	Images	inode/directory	t	1
302	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion 1	inode/directory	t	\N
303	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	302	Images	inode/directory	t	1
304	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	303	Megaptera.jpg	image/jpeg	f	1
305	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	303	Slovakia.jpg	image/jpeg	f	1
306	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	302	Megaptera 2.jpg	image/jpeg	f	1
309	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	308	Megaptera.jpg	image/jpeg	f	1
310	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	308	Slovakia.jpg	image/jpeg	f	1
308	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	307	Images	inode/directory	t	1
311	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	307	Megaptera 2.jpg	image/jpeg	f	1
312	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion	inode/directory	t	\N
313	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	312	Megaptera 2.jpg	image/jpeg	f	1
314	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	312	Images	inode/directory	t	1
315	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	314	Slovakia.jpg	image/jpeg	f	1
316	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	314	Megaptera.jpg	image/jpeg	f	1
317	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	informacion	inode/directory	t	\N
318	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	317	Megaptera 2.jpg	image/jpeg	f	1
319	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	317	Images	inode/directory	t	1
320	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	319	Slovakia.jpg	image/jpeg	f	1
321	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	319	Megaptera.jpg	image/jpeg	f	1
307	a0d404a4-7240-4750-89b2-3c01601b0c1d	6	\N	informacion	inode/directory	t	\N
465	85abf38c-20c1-4116-a1c1-20112d23e67b	2	462	D6.3_Cloudspaces_Public.pdf	application/pdf	f	1
326	d9c10e45-002b-4240-804a-3b4e24497e5c	1	\N	20131115_141306.jpg	image/jpeg	f	\N
327	d9c10e45-002b-4240-804a-3b4e24497e5c	2	6	1024X600_01.jpg	image/jpeg	f	1
328	d9c10e45-002b-4240-804a-3b4e24497e5c	1	6	1024X600_01.jpg	image/jpeg	f	1
330	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	stacksync_2.0_all.deb	unknown	f	\N
418	b944f1d0-0c5e-4c2c-a170-2d84a61cc951	2	\N	cotes	unknown	t	\N
76	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	5	71	Megaptera 2.jpg	image/jpeg	f	1
334	6f53e591-9019-4aa7-b8ae-fa63cb9f1035	1	\N	Documents	inode/directory	t	\N
335	6f53e591-9019-4aa7-b8ae-fa63cb9f1035	1	\N	IMG-20140716-WA0005.jpg	image/jpeg	f	\N
117	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	1	116	Slovakia.jpg	image/jpeg	f	1
118	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	1	116	Megaptera.jpg	image/jpeg	f	1
323	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	116	New Document.edoc	application/zip	f	1
322	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	116	New Folder	inode/directory	t	1
324	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	116	ggggg.txt	text/plain	f	1
331	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	116	New Folder	inode/directory	t	1
108	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	3	71	sebas	inode/directory	t	3
184	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	3	71	Prueba	inode/directory	t	3
213	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	71	prueba.txt	text/plain	f	3
191	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	3	71	Prueba	inode/directory	t	3
219	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	71	Prueba 1.txt	text/plain	f	3
220	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	71	Prueba 2.txt	text/plain	f	3
221	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	71	Prueba 3.txt	text/plain	f	3
222	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	71	Prueba 4.txt	text/plain	f	3
223	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	71	Prueba 5.txt	text/plain	f	3
224	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	71	Prueba 6.txt	text/plain	f	3
218	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	71	Prueba.txt	text/plain	f	3
257	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	256	Images	inode/directory	t	1
128	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	3	71	Images	inode/directory	t	2
325	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	322	yyyy.txt	text/plain	f	1
129	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	128	Slovakia.jpg	image/jpeg	f	1
130	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	128	Megaptera.jpg	image/jpeg	f	1
185	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	184	prueba1.txt	text/plain	f	2
186	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	184	prueba2.txt	text/plain	f	2
187	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	184	a.txt	text/plain	f	2
188	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	184	b.txt	text/plain	f	2
189	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	184	c.txt	text/plain	f	2
332	c644dfd0-8c02-45e6-8b95-e8da93183ded	2	\N	Informacion	inode/directory	t	\N
333	3f76fbe0-8c37-48b5-891a-0dd9d0e61456	2	\N	Otro	inode/directory	t	\N
75	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	4	71	null	image/jpeg	f	1
77	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	3	71	Megaptera 3.jpg	image/jpeg	f	1
91	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	3	71	null	image/jpeg	f	1
98	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	3	71	Megaptera 2 1 1.jpg	image/jpeg	f	1
472	b4d4eb00-34d7-46c0-976f-dba69a58283f	1	\N	images	unknown	t	\N
428	4fa20d25-1dd4-44de-91eb-71fc6d784e79	2	430	IMG-20140716-WA0005.jpg	image/jpeg	f	1
434	c6f1148d-9e0f-426a-b48c-8aef0ebb818d	2	433	Untitled Document	text/plain	f	2
435	c6f1148d-9e0f-426a-b48c-8aef0ebb818d	1	433	Untitled Document	text/plain	f	2
473	b4d4eb00-34d7-46c0-976f-dba69a58283f	1	472	arboles-nevados.jpg	image/jpeg	f	1
445	cda89cef-4471-40cf-93cf-559fd52ea0e8	2	\N	share2	unknown	t	\N
447	cda89cef-4471-40cf-93cf-559fd52ea0e8	1	445	Getting Started.pdf	application/pdf	f	2
442	244c075f-b71f-4424-a961-4b2b9890d4de	3	\N	asfdsa	text/plain	f	\N
437	244c075f-b71f-4424-a961-4b2b9890d4de	3	\N	aaaa	unknown	t	\N
453	a0d404a4-7240-4750-89b2-3c01601b0c1d	1	\N	actively_measuring.pdf	application/pdf	f	\N
466	85abf38c-20c1-4116-a1c1-20112d23e67b	2	462	D4.2_Cloudspaces_Public.pdf	application/pdf	f	1
464	85abf38c-20c1-4116-a1c1-20112d23e67b	2	462	D5.2_Cloudspaces_Public.pdf	application/pdf	f	1
190	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	184	d.txt	text/plain	f	2
192	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	3	191	Prueba1	inode/directory	t	2
194	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	3	191	Prueba2	inode/directory	t	2
195	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	Fichero1 1.txt	text/plain	f	2
193	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	Fichero1.txt	text/plain	f	2
197	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	a1 1.txt	text/plain	f	2
198	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	a1 2.txt	text/plain	f	2
199	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	a1 3.txt	text/plain	f	2
200	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	a1 4.txt	text/plain	f	2
201	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	a1 5.txt	text/plain	f	2
202	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	a1 6.txt	text/plain	f	2
196	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	a1.txt	text/plain	f	2
204	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	b 1.txt	text/plain	f	2
205	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	b 2.txt	text/plain	f	2
206	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	b 3.txt	text/plain	f	2
207	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	b 4.txt	text/plain	f	2
208	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	b 5.txt	text/plain	f	2
209	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	b 6.txt	text/plain	f	2
210	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	b 7.txt	text/plain	f	2
211	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	b 8.txt	text/plain	f	2
212	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	b 9.txt	text/plain	f	2
203	ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	2	191	b.txt	text/plain	f	2
362	d9c10e45-002b-4240-804a-3b4e24497e5c	1	6	api.pdf	application/pdf	f	1
366	d9c10e45-002b-4240-804a-3b4e24497e5c	3	\N	AAA.jpg	image/jpeg	f	\N
392	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	pgadmin.log	text/plain	f	\N
338	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Fighter.mpg	video/mpeg	f	\N
339	a0d404a4-7240-4750-89b2-3c01601b0c1d	2	\N	Fighter.mpg	video/mpeg	f	\N
340	c644dfd0-8c02-45e6-8b95-e8da93183ded	1	332	Fighter.mpg	video/mpeg	f	2
341	c644dfd0-8c02-45e6-8b95-e8da93183ded	1	332	barsandtone.flv	video/x-flv	f	2
342	c644dfd0-8c02-45e6-8b95-e8da93183ded	1	332	20051210-w50s.flv	video/x-flv	f	2
343	c644dfd0-8c02-45e6-8b95-e8da93183ded	1	332	mpthreetest.mp3	audio/mpeg	f	2
344	c644dfd0-8c02-45e6-8b95-e8da93183ded	1	332	video1.avi	video/x-msvideo	f	2
474	b4d4eb00-34d7-46c0-976f-dba69a58283f	1	472	arboles.jpg	image/jpeg	f	1
475	b4d4eb00-34d7-46c0-976f-dba69a58283f	1	472	paisajes-nevados-1.jpg	image/jpeg	f	1
476	b4d4eb00-34d7-46c0-976f-dba69a58283f	1	472	estampa-nevada.jpg	image/jpeg	f	1
477	b4d4eb00-34d7-46c0-976f-dba69a58283f	1	472	paisajes-playas-palmeras.jpg	image/jpeg	f	1
352	b150400f-70d4-401b-b28f-6617536d43df	1	\N	Documents	inode/directory	t	\N
353	b150400f-70d4-401b-b28f-6617536d43df	1	\N	Pictures	inode/directory	t	\N
354	b150400f-70d4-401b-b28f-6617536d43df	1	353	IMG-20140809-WA0001.jpg	image/jpeg	f	1
419	b944f1d0-0c5e-4c2c-a170-2d84a61cc951	1	418	IMG-20140716-WA0005.jpg	image/jpeg	f	2
355	3f76fbe0-8c37-48b5-891a-0dd9d0e61456	1	333	Images	inode/directory	t	2
358	3f76fbe0-8c37-48b5-891a-0dd9d0e61456	1	333	add.txt	text/plain	f	2
357	3f76fbe0-8c37-48b5-891a-0dd9d0e61456	3	355	Slovakia.jpg	image/jpeg	f	1
356	3f76fbe0-8c37-48b5-891a-0dd9d0e61456	3	355	Megaptera.jpg	image/jpeg	f	1
429	4fa20d25-1dd4-44de-91eb-71fc6d784e79	7	427	pgadmin.log	text/plain	f	2
359	1e6b3715-e787-4685-b81e-cd0ac9b4f7f0	1	\N	shared	inode/directory	t	\N
361	d9c10e45-002b-4240-804a-3b4e24497e5c	1	359	cloudspaces-logo-square-big.png	image/png	f	1
365	d9c10e45-002b-4240-804a-3b4e24497e5c	3	\N	LOKO.jpg	image/jpeg	f	\N
364	d9c10e45-002b-4240-804a-3b4e24497e5c	3	\N	noparent.jpg	image/jpeg	f	\N
367	d9c10e45-002b-4240-804a-3b4e24497e5c	2	6	historia_buena.jpg	image/jpeg	f	1
363	d9c10e45-002b-4240-804a-3b4e24497e5c	3	\N	bbb	inode/directory	t	\N
446	cda89cef-4471-40cf-93cf-559fd52ea0e8	1	445	bladfa.png	image/png	f	2
370	1e6b3715-e787-4685-b81e-cd0ac9b4f7f0	1	359	aaa	inode/directory	t	1
371	1e6b3715-e787-4685-b81e-cd0ac9b4f7f0	1	359	a2-ju.pdf	application/pdf	f	1
439	244c075f-b71f-4424-a961-4b2b9890d4de	3	\N	fadf	unknown	t	\N
454	a0d404a4-7240-4750-89b2-3c01601b0c1d	1	\N	cloudabe.pdf	application/pdf	f	\N
456	1e6b3715-e787-4685-b81e-cd0ac9b4f7f0	2	\N	moved.jpg	image/jpeg	f	\N
373	d9c10e45-002b-4240-804a-3b4e24497e5c	2	\N	shared3	inode/directory	t	\N
467	85abf38c-20c1-4116-a1c1-20112d23e67b	2	462	D3.2_Cloudspaces_Public.pdf	application/pdf	f	1
391	d9c10e45-002b-4240-804a-3b4e24497e5c	1	\N	cert.crt	text/plain	f	2
\.


--
-- Data for Name: item_version; Type: TABLE DATA; Schema: public; Owner: stacksync_user
--

COPY item_version (id, item_id, device_id, version, committed_at, checksum, modified_at, status, size) FROM stdin;
1	1	00000000-0000-0001-0000-000000000001	1	2014-05-28 12:54:06.753901	0	2014-05-28 12:54:06.749	NEW	0
2	2	00000000-0000-0001-0000-000000000001	1	2014-05-28 14:13:36.625612	2190678904	2014-05-28 14:13:36.617	NEW	78
3	3	00000000-0000-0001-0000-000000000001	1	2014-05-28 15:29:21.823549	2190678904	2014-05-28 15:29:21.807	NEW	78
5	5	b671c49d-61b6-43ce-9847-a169ca618dae	1	2014-06-03 10:32:24.308602	0	2014-06-03 10:38:38	NEW	4096
6	5	b671c49d-61b6-43ce-9847-a169ca618dae	2	2014-06-03 10:32:34.262262	0	2014-06-03 10:38:38	DELETED	4096
7	6	5e8e71bd-5b7c-4520-96dc-bf72eeeed594	1	2014-06-03 12:45:06.23699	0	2013-12-02 16:34:29	NEW	4096
8	7	5e8e71bd-5b7c-4520-96dc-bf72eeeed594	1	2014-06-03 12:45:06.257172	0	2013-11-21 11:45:14	NEW	4096
9	8	5e8e71bd-5b7c-4520-96dc-bf72eeeed594	1	2014-06-03 12:45:06.274201	0	2013-12-09 10:06:40	NEW	4096
10	9	5e8e71bd-5b7c-4520-96dc-bf72eeeed594	1	2014-06-03 12:45:11.319539	4285644848	2013-07-10 17:43:21	NEW	72273
11	10	5e8e71bd-5b7c-4520-96dc-bf72eeeed594	1	2014-06-03 12:45:11.344265	663039848	2013-07-10 17:41:20	NEW	516264
12	11	5e8e71bd-5b7c-4520-96dc-bf72eeeed594	1	2014-06-03 12:45:11.360964	4091858297	2013-07-10 17:41:43	NEW	332633
13	12	5e8e71bd-5b7c-4520-96dc-bf72eeeed594	1	2014-06-03 12:45:11.378299	72337213	2013-07-10 17:42:00	NEW	1044542
14	13	5e8e71bd-5b7c-4520-96dc-bf72eeeed594	1	2014-06-03 12:45:11.394273	3021324517	2013-07-10 17:42:14	NEW	869606
15	14	5e8e71bd-5b7c-4520-96dc-bf72eeeed594	1	2014-06-03 12:45:11.410832	1595847508	2013-07-10 17:41:34	NEW	499147
16	15	00000000-0000-0001-0000-000000000001	1	2014-06-03 17:52:00.795835	45416794	2014-06-03 17:52:00.785	NEW	3
17	16	00000000-0000-0001-0000-000000000001	1	2014-06-03 17:58:04.451836	75629005	2014-06-03 17:58:04.44	NEW	4
18	17	00000000-0000-0001-0000-000000000001	1	2014-06-03 18:29:14.275299	151519872	2014-06-03 18:29:14.265	NEW	6
19	18	00000000-0000-0001-0000-000000000001	1	2014-06-03 18:38:25.074268	2848328517	2014-06-03 18:38:25.063	NEW	29
20	19	00000000-0000-0001-0000-000000000001	1	2014-06-03 18:43:05.467678	477430984	2014-06-03 18:43:05.458	NEW	11
21	20	00000000-0000-0001-0000-000000000001	1	2014-06-03 18:45:43.24931	477430984	2014-06-03 18:45:43.237	NEW	11
22	21	00000000-0000-0001-0000-000000000001	1	2014-06-03 18:46:17.713149	477430984	2014-06-03 18:46:17.701	NEW	11
23	22	00000000-0000-0001-0000-000000000001	1	2014-06-03 18:48:08.013165	1510738075	2014-06-03 18:48:08.001	NEW	20
24	23	00000000-0000-0001-0000-000000000001	1	2014-06-03 18:54:15.372924	190448151	2014-06-03 18:54:15.36	NEW	10
25	24	00000000-0000-0001-0000-000000000001	1	2014-06-03 18:55:28.343197	223216226	2014-06-03 18:55:28.331	NEW	9
26	25	00000000-0000-0001-0000-000000000001	1	2014-06-04 10:28:19.940051	75629005	2014-06-04 10:28:19.928	NEW	4
27	26	00000000-0000-0001-0000-000000000001	1	2014-06-04 10:41:12.643378	75629005	2014-06-04 10:41:12.631	NEW	4
28	27	00000000-0000-0001-0000-000000000001	1	2014-06-04 10:43:06.535194	45416794	2014-06-04 10:43:06.523	NEW	3
29	28	00000000-0000-0001-0000-000000000001	1	2014-06-04 11:00:49.654935	45416794	2014-06-04 11:00:49.643	NEW	3
30	29	00000000-0000-0001-0000-000000000001	1	2014-06-04 11:01:59.306358	95683046	2014-06-04 11:01:59.293	NEW	5
31	30	00000000-0000-0001-0000-000000000001	1	2014-06-04 13:11:45.743725	643892621	2014-06-04 13:11:45.724	NEW	13
32	31	00000000-0000-0001-0000-000000000001	1	2014-06-04 14:45:45.074235	0	2014-06-04 14:45:45.071	NEW	0
33	32	00000000-0000-0001-0000-000000000001	1	2014-06-04 14:51:26.586922	0	2014-06-04 14:51:26.583	NEW	0
34	33	00000000-0000-0001-0000-000000000001	1	2014-06-04 15:39:04.45295	0	2014-06-04 15:39:04.447	NEW	0
35	34	00000000-0000-0001-0000-000000000001	1	2014-06-04 15:40:45.299012	0	2014-06-04 15:40:45.293	NEW	0
36	17	00000000-0000-0001-0000-000000000001	2	2014-06-04 15:56:11.113713	231473954	2014-06-03 18:29:14.265	CHANGED	8
37	17	00000000-0000-0001-0000-000000000001	3	2014-06-04 15:58:04.121978	231473954	2014-06-03 18:29:14.265	CHANGED	8
38	17	00000000-0000-0001-0000-000000000001	4	2014-06-04 15:58:30.953261	231473954	2014-06-03 18:29:14.265	CHANGED	8
39	17	00000000-0000-0001-0000-000000000001	5	2014-06-04 15:59:02.168039	231473954	2014-06-03 18:29:14.265	CHANGED	8
40	17	00000000-0000-0001-0000-000000000001	6	2014-06-04 16:01:16.47782	231473954	2014-06-03 18:29:14.265	CHANGED	8
41	17	00000000-0000-0001-0000-000000000001	7	2014-06-04 16:03:48.893816	322569172	2014-06-03 18:29:14.265	CHANGED	9
42	17	00000000-0000-0001-0000-000000000001	8	2014-06-04 16:08:08.484451	913901171	2014-06-03 18:29:14.265	CHANGED	16
43	17	00000000-0000-0001-0000-000000000001	9	2014-06-04 16:10:16.437815	257557349	2014-06-03 18:29:14.265	CHANGED	8
44	17	00000000-0000-0001-0000-000000000001	10	2014-06-04 16:14:51.023481	262275960	2014-06-03 18:29:14.265	CHANGED	8
45	17	00000000-0000-0001-0000-000000000001	11	2014-06-04 16:17:26.356979	326829017	2014-06-03 18:29:14.265	CHANGED	9
46	17	00000000-0000-0001-0000-000000000001	12	2014-06-04 16:18:08.42475	397739066	2014-06-03 18:29:14.265	CHANGED	10
47	20	00000000-0000-0001-0000-000000000001	2	2014-06-04 16:57:06.326588	477430984	2014-06-04 16:57:06.32	DELETED	11
48	21	00000000-0000-0001-0000-000000000001	2	2014-06-04 16:59:42.127869	477430984	2014-06-04 16:59:42.122	DELETED	11
49	33	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:00:57.671852	0	2014-06-04 17:00:57.668	DELETED	0
50	35	00000000-0000-0001-0000-000000000001	1	2014-06-04 17:02:06.863891	0	2014-06-04 17:02:06.859	NEW	0
51	35	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:03:23.841591	0	2014-06-04 17:03:23.838	DELETED	0
52	30	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:04:00.601334	643892621	2014-06-04 17:04:00.597	DELETED	13
53	24	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:04:59.472672	223216226	2014-06-04 17:04:59.469	DELETED	9
54	25	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:05:00.360649	75629005	2014-06-04 17:05:00.357	DELETED	4
55	26	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:05:01.228557	75629005	2014-06-04 17:05:01.224	DELETED	4
56	28	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:05:02.144146	45416794	2014-06-04 17:05:02.14	DELETED	3
57	15	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:05:03.008329	45416794	2014-06-04 17:05:03.004	DELETED	3
58	16	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:05:03.850675	75629005	2014-06-04 17:05:03.846	DELETED	4
59	32	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:05:43.777362	0	2014-06-04 17:05:43.773	DELETED	0
60	34	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:05:43.792337	0	2014-06-04 17:05:43.773	DELETED	0
61	31	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:05:44.662197	0	2014-06-04 17:05:44.658	DELETED	0
62	18	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:06:29.785501	2848328517	2014-06-04 17:06:29.782	DELETED	29
63	19	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:06:30.64827	477430984	2014-06-04 17:06:30.644	DELETED	11
64	22	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:06:31.520134	1510738075	2014-06-04 17:06:31.516	DELETED	20
65	23	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:06:32.337684	190448151	2014-06-04 17:06:32.334	DELETED	10
66	27	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:06:33.202012	45416794	2014-06-04 17:06:33.198	DELETED	3
67	29	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:06:34.060989	95683046	2014-06-04 17:06:34.057	DELETED	5
68	17	00000000-0000-0001-0000-000000000001	13	2014-06-04 17:06:34.923805	397739066	2014-06-04 17:06:34.918	DELETED	10
69	36	00000000-0000-0001-0000-000000000001	1	2014-06-04 17:08:15.430092	310001724	2014-06-04 17:08:15.414	NEW	141750
70	36	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:09:36.973245	310001724	2014-06-04 17:09:36.969	DELETED	141750
71	37	00000000-0000-0001-0000-000000000001	1	2014-06-04 17:10:22.820537	2999639044	2014-06-04 17:10:22.805	NEW	7991
72	37	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:14:52.431448	2999639044	2014-06-04 17:14:52.428	DELETED	7991
73	38	00000000-0000-0001-0000-000000000001	1	2014-06-04 17:15:08.033574	574913855	2014-06-04 17:15:08.02	NEW	71883
74	38	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:15:40.045494	574913855	2014-06-04 17:15:40.042	DELETED	71883
75	39	00000000-0000-0001-0000-000000000001	1	2014-06-04 17:20:08.346104	574913855	2014-06-04 17:20:08.333	NEW	71883
76	39	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:30:48.480442	574913855	2014-06-04 17:30:48.477	DELETED	71883
77	40	00000000-0000-0001-0000-000000000001	1	2014-06-04 17:31:02.203371	574913855	2014-06-04 17:31:02.19	NEW	71883
78	40	00000000-0000-0001-0000-000000000001	2	2014-06-04 17:33:28.749071	574913855	2014-06-04 17:33:28.746	DELETED	71883
79	41	00000000-0000-0001-0000-000000000001	1	2014-06-04 17:33:46.398638	574913855	2014-06-04 17:33:46.386	NEW	71883
80	42	00000000-0000-0001-0000-000000000001	1	2014-06-04 17:34:22.0319	158663347	2014-06-04 17:34:22.024	NEW	6
81	43	00000000-0000-0001-0000-000000000001	1	2014-06-05 10:22:40.103536	0	2014-06-05 10:22:40.099	NEW	0
82	44	00000000-0000-0001-0000-000000000001	1	2014-06-05 10:23:05.49563	75629005	2014-06-05 10:23:05.486	NEW	4
83	41	00000000-0000-0001-0000-000000000001	2	2014-06-05 11:18:18.489164	574913855	2014-06-05 11:18:18.486	DELETED	71883
84	45	00000000-0000-0001-0000-000000000001	1	2014-06-05 11:19:09.223937	574913855	2014-06-05 11:19:09.211	NEW	71883
85	45	00000000-0000-0001-0000-000000000001	2	2014-06-05 12:41:02.726	574913855	2014-06-05 12:41:02.723	DELETED	71883
86	46	00000000-0000-0001-0000-000000000001	1	2014-06-05 12:41:23.046599	574913855	2014-06-05 12:41:23.035	NEW	71883
87	46	00000000-0000-0001-0000-000000000001	2	2014-06-05 12:45:52.26169	574913855	2014-06-05 12:45:52.258	DELETED	71883
88	47	00000000-0000-0001-0000-000000000001	1	2014-06-05 12:46:07.357357	574913855	2014-06-05 12:46:07.345	NEW	71883
89	47	00000000-0000-0001-0000-000000000001	2	2014-06-05 12:56:39.544848	574913855	2014-06-05 12:56:39.542	DELETED	71883
90	48	00000000-0000-0001-0000-000000000001	1	2014-06-05 12:56:55.127052	574913855	2014-06-05 12:56:55.114	NEW	71883
91	48	00000000-0000-0001-0000-000000000001	2	2014-06-05 12:58:33.748686	574913855	2014-06-05 12:58:33.745	DELETED	71883
92	49	00000000-0000-0001-0000-000000000001	1	2014-06-05 12:58:45.178998	574913855	2014-06-05 12:58:45.166	NEW	71883
93	42	00000000-0000-0001-0000-000000000001	2	2014-06-05 15:06:55.046116	688129807	2014-06-04 17:34:22.024	CHANGED	12
94	50	00000000-0000-0001-0000-000000000001	1	2014-06-05 15:09:50.673488	113377856	2014-06-05 15:09:50.661	NEW	5
95	50	00000000-0000-0001-0000-000000000001	2	2014-06-05 15:10:42.382745	113377856	2014-06-05 15:10:42.379	DELETED	5
96	51	00000000-0000-0001-0000-000000000001	1	2014-06-05 15:11:01.499833	0	2014-06-05 15:11:01.497	NEW	0
97	51	00000000-0000-0001-0000-000000000001	2	2014-06-05 15:11:25.876338	0	2014-06-05 15:11:25.873	DELETED	0
98	49	00000000-0000-0001-0000-000000000001	2	2014-06-05 15:11:42.335452	574913855	2014-06-05 15:11:42.332	DELETED	71883
99	52	00000000-0000-0001-0000-000000000001	1	2014-06-05 15:12:00.067086	574913855	2014-06-05 15:12:00.054	NEW	71883
100	53	00000000-0000-0001-0000-000000000001	1	2014-06-05 15:55:53.654012	0	2014-06-05 15:55:53.652	NEW	0
101	53	00000000-0000-0001-0000-000000000001	2	2014-06-05 16:00:09.813942	0	2014-06-05 16:00:09.811	DELETED	0
102	54	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:00:30.248187	0	2014-06-05 16:00:30.245	NEW	0
103	55	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:00:31.708177	252178708	2014-06-05 16:00:31.699	NEW	94338
104	56	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:00:33.049511	574913855	2014-06-05 16:00:33.04	NEW	71883
105	57	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:04:26.861455	0	2014-06-05 16:04:26.858	NEW	0
106	58	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:04:28.468502	574913855	2014-06-05 16:04:28.46	NEW	71883
107	59	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:04:30.24254	252178708	2014-06-05 16:04:30.234	NEW	94338
108	60	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:09:19.375237	0	2014-06-05 16:09:19.371	NEW	0
109	61	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:09:20.329882	0	2014-06-05 16:09:20.326	NEW	0
110	62	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:09:22.041313	252178708	2014-06-05 16:09:22.033	NEW	94338
111	63	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:09:23.758121	574913855	2014-06-05 16:09:23.749	NEW	71883
112	64	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:09:25.149236	75629005	2014-06-05 16:09:25.14	NEW	4
113	65	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:11:27.106888	688129807	2014-06-05 16:11:27.097	NEW	12
114	66	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:11:28.16642	0	2014-06-05 16:11:28.162	NEW	0
115	67	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:11:29.543948	75629005	2014-06-05 16:11:29.536	NEW	4
116	68	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:11:30.509848	0	2014-06-05 16:11:30.506	NEW	0
117	69	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:11:32.232969	574913855	2014-06-05 16:11:32.225	NEW	71883
118	70	00000000-0000-0001-0000-000000000001	1	2014-06-05 16:11:34.060047	252178708	2014-06-05 16:11:34.051	NEW	94338
119	42	00000000-0000-0001-0000-000000000001	3	2014-06-05 18:03:19.317394	688129807	2014-06-04 17:34:22.024	CHANGED	12
120	71	00000000-0000-0001-0000-000000000001	1	2014-06-06 09:55:32.87628	0	2014-06-06 09:55:32.873	NEW	0
121	42	00000000-0000-0001-0000-000000000001	4	2014-06-06 11:03:57.321537	688129807	2014-06-06 11:03:57.319	DELETED	12
122	65	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:06:41.281131	688129807	2014-06-06 11:06:41.278	DELETED	12
123	72	00000000-0000-0001-0000-000000000001	1	2014-06-06 11:07:39.123571	40304947	2014-06-06 11:07:39.114	NEW	3
124	72	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:07:43.905404	40304947	2014-06-06 11:07:43.902	DELETED	3
125	73	00000000-0000-0001-0000-000000000001	1	2014-06-06 11:12:46.221611	113377856	2014-06-06 11:12:46.214	NEW	5
126	73	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:12:53.568058	113377856	2014-06-06 11:12:53.565	DELETED	5
127	66	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:13:16.894354	0	2014-06-06 11:13:16.891	DELETED	0
128	67	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:13:16.911731	75629005	2014-06-06 11:13:16.891	DELETED	4
129	68	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:13:16.928353	0	2014-06-06 11:13:16.891	DELETED	0
130	69	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:13:16.945056	574913855	2014-06-06 11:13:16.891	DELETED	71883
131	70	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:13:16.961715	252178708	2014-06-06 11:13:16.891	DELETED	94338
132	60	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:14:14.014972	0	2014-06-06 11:14:14.012	DELETED	0
133	61	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:14:14.034255	0	2014-06-06 11:14:14.012	DELETED	0
134	62	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:14:14.050915	252178708	2014-06-06 11:14:14.012	DELETED	94338
135	63	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:14:14.067539	574913855	2014-06-06 11:14:14.012	DELETED	71883
136	64	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:14:14.084183	75629005	2014-06-06 11:14:14.012	DELETED	4
137	74	00000000-0000-0001-0000-000000000001	1	2014-06-06 11:14:53.983537	0	2014-06-06 11:14:53.981	NEW	0
138	74	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:14:59.568594	0	2014-06-06 11:14:59.566	DELETED	0
139	75	00000000-0000-0001-0000-000000000001	1	2014-06-06 11:47:54.081336	574913855	2014-06-06 11:47:54.069	NEW	71883
140	76	00000000-0000-0001-0000-000000000001	1	2014-06-06 11:51:17.388523	574913855	2014-06-06 11:51:17.376	NEW	71883
141	76	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:52:23.735697	574913855	2014-06-06 11:51:17.376	CHANGED	71883
142	76	00000000-0000-0001-0000-000000000001	3	2014-06-06 11:53:05.727899	574913855	2014-06-06 11:51:17.376	CHANGED	71883
143	77	00000000-0000-0001-0000-000000000001	1	2014-06-06 11:58:19.978581	574913855	2014-06-06 11:58:19.966	NEW	71883
144	78	00000000-0000-0001-0000-000000000001	1	2014-06-06 11:59:04.796009	574913855	2014-06-06 11:59:04.783	NEW	71883
145	78	00000000-0000-0001-0000-000000000001	2	2014-06-06 11:59:12.356707	574913855	2014-06-06 11:59:04.783	CHANGED	71883
146	78	00000000-0000-0001-0000-000000000001	3	2014-06-06 11:59:27.618945	574913855	2014-06-06 11:59:04.783	CHANGED	71883
147	79	00000000-0000-0001-0000-000000000001	1	2014-06-06 12:01:03.160643	574913855	2014-06-06 12:01:03.148	NEW	71883
148	80	00000000-0000-0001-0000-000000000001	1	2014-06-06 12:01:41.889014	574913855	2014-06-06 12:01:41.876	NEW	71883
149	80	00000000-0000-0001-0000-000000000001	2	2014-06-06 12:02:18.130413	574913855	2014-06-06 12:01:41.876	CHANGED	71883
150	81	00000000-0000-0001-0000-000000000001	1	2014-06-06 12:04:07.333742	574913855	2014-06-06 12:04:07.322	NEW	71883
151	82	00000000-0000-0001-0000-000000000001	1	2014-06-06 12:14:04.562234	1480984607	2014-06-06 12:14:04.552	NEW	20
152	79	00000000-0000-0001-0000-000000000001	2	2014-06-06 12:21:18.540458	574913855	2014-06-06 12:21:18.538	DELETED	71883
153	75	00000000-0000-0001-0000-000000000001	2	2014-06-06 17:06:47.314335	574913855	2014-06-06 11:47:54.069	RENAMED	71883
154	71	00000000-0000-0001-0000-000000000001	2	2014-06-06 17:09:01.705484	0	2014-06-06 09:55:32.873	RENAMED	0
155	1	00000000-0000-0001-0000-000000000001	2	2014-06-06 17:10:02.453019	0	2014-05-28 12:54:06.749	RENAMED	0
156	43	00000000-0000-0001-0000-000000000001	2	2014-06-06 17:13:47.45472	0	2014-06-05 10:22:40.099	RENAMED	0
157	83	00000000-0000-0001-0000-000000000001	1	2014-06-06 17:14:04.203244	63832453	2014-06-06 17:14:04.194	NEW	4
158	83	00000000-0000-0001-0000-000000000001	2	2014-06-06 17:14:18.263667	63832453	2014-06-06 17:14:04.194	RENAMED	4
159	83	00000000-0000-0001-0000-000000000001	3	2014-06-06 17:15:57.129752	75629005	2014-06-06 17:14:04.194	CHANGED	4
160	83	00000000-0000-0001-0000-000000000001	4	2014-06-06 17:16:43.363605	95683046	2014-06-06 17:14:04.194	CHANGED	5
161	43	00000000-0000-0001-0000-000000000001	3	2014-06-06 17:21:19.418175	0	2014-06-05 10:22:40.099	RENAMED	0
162	84	00000000-0000-0001-0000-000000000001	1	2014-06-06 17:21:34.451552	1981548946	2014-06-06 17:21:34.443	NEW	24
163	84	00000000-0000-0001-0000-000000000001	2	2014-06-06 17:21:47.157533	1981548946	2014-06-06 17:21:34.443	RENAMED	24
164	71	00000000-0000-0001-0000-000000000001	3	2014-06-06 17:29:44.967156	0	2014-06-06 09:55:32.873	RENAMED	0
165	85	00000000-0000-0001-0000-000000000001	1	2014-06-10 09:43:34.852688	574913855	2014-06-10 09:43:34.842	NEW	71883
166	81	00000000-0000-0001-0000-000000000001	2	2014-06-10 09:44:13.939651	574913855	2014-06-06 12:04:07.322	RENAMED	71883
167	86	410843cb-b4a4-4ad1-93bb-099299d79a48	1	2014-06-10 11:26:59.565878	663039848	2013-07-10 17:41:20	NEW	516264
168	87	410843cb-b4a4-4ad1-93bb-099299d79a48	1	2014-06-10 11:26:59.621625	4091858297	2013-07-10 17:41:43	NEW	332633
169	88	410843cb-b4a4-4ad1-93bb-099299d79a48	1	2014-06-10 11:26:59.63852	72337213	2013-07-10 17:42:00	NEW	1044542
170	89	410843cb-b4a4-4ad1-93bb-099299d79a48	1	2014-06-10 11:26:59.655143	3021324517	2013-07-10 17:42:14	NEW	869606
171	90	410843cb-b4a4-4ad1-93bb-099299d79a48	1	2014-06-10 11:26:59.67181	1595847508	2013-07-10 17:41:34	NEW	499147
172	14	410843cb-b4a4-4ad1-93bb-099299d79a48	2	2014-06-10 11:26:59.687663	1595847508	2013-07-10 17:41:34	RENAMED	499147
173	13	410843cb-b4a4-4ad1-93bb-099299d79a48	2	2014-06-10 11:26:59.71183	3021324517	2013-07-10 17:42:14	RENAMED	869606
174	12	410843cb-b4a4-4ad1-93bb-099299d79a48	2	2014-06-10 11:26:59.736805	72337213	2013-07-10 17:42:00	RENAMED	1044542
175	11	410843cb-b4a4-4ad1-93bb-099299d79a48	2	2014-06-10 11:26:59.761825	4091858297	2013-07-10 17:41:43	RENAMED	332633
176	10	410843cb-b4a4-4ad1-93bb-099299d79a48	2	2014-06-10 11:26:59.786756	663039848	2013-07-10 17:41:20	RENAMED	516264
177	90	410843cb-b4a4-4ad1-93bb-099299d79a48	2	2014-06-10 11:27:19.47453	1595847508	2013-07-10 17:41:34	DELETED	499147
178	12	410843cb-b4a4-4ad1-93bb-099299d79a48	3	2014-06-10 11:27:19.491917	72337213	2013-07-10 17:42:00	DELETED	1044542
179	11	410843cb-b4a4-4ad1-93bb-099299d79a48	3	2014-06-10 11:27:19.508625	4091858297	2013-07-10 17:41:43	DELETED	332633
180	10	410843cb-b4a4-4ad1-93bb-099299d79a48	3	2014-06-10 11:27:19.532656	663039848	2013-07-10 17:41:20	DELETED	516264
181	13	410843cb-b4a4-4ad1-93bb-099299d79a48	3	2014-06-10 11:27:19.557638	3021324517	2013-07-10 17:42:14	DELETED	869606
182	14	0aa5d20c-5d16-414f-97c2-c5ef248e976d	3	2014-06-10 11:41:46.124291	1595847508	2013-07-10 17:41:34	RENAMED	499147
183	78	00000000-0000-0001-0000-000000000001	4	2014-06-10 11:44:15.511539	574913855	2014-06-06 11:59:04.783	RENAMED	71883
184	80	00000000-0000-0001-0000-000000000001	3	2014-06-10 12:45:50.444096	574913855	2014-06-06 12:01:41.876	RENAMED	71883
185	91	00000000-0000-0001-0000-000000000001	1	2014-06-10 14:53:20.490731	574913855	2014-06-10 14:53:20.476	NEW	71883
186	92	00000000-0000-0001-0000-000000000001	1	2014-06-10 14:54:03.872163	574913855	2014-06-10 14:54:03.858	NEW	71883
187	81	00000000-0000-0001-0000-000000000001	3	2014-06-10 15:49:13.869413	574913855	2014-06-06 12:04:07.322	RENAMED	71883
188	91	00000000-0000-0001-0000-000000000001	2	2014-06-10 16:40:53.028389	574913855	2014-06-10 14:53:20.476	RENAMED	71883
189	85	00000000-0000-0001-0000-000000000001	2	2014-06-10 16:46:38.58332	574913855	2014-06-10 09:43:34.842	CHANGED	71883
190	85	00000000-0000-0001-0000-000000000001	3	2014-06-10 16:47:17.612073	574913855	2014-06-10 09:43:34.842	CHANGED	71883
191	93	00000000-0000-0001-0000-000000000001	1	2014-06-10 16:59:04.686903	113377856	2014-06-10 16:59:04.674	NEW	5
192	93	00000000-0000-0001-0000-000000000001	2	2014-06-10 16:59:50.906624	113377856	2014-06-10 16:59:04.674	RENAMED	5
193	81	00000000-0000-0001-0000-000000000001	4	2014-06-10 18:04:37.699349	574913855	2014-06-06 12:04:07.322	RENAMED	71883
194	92	00000000-0000-0001-0000-000000000001	2	2014-06-11 09:50:15.788438	574913855	2014-06-11 09:50:15.783	DELETED	71883
195	85	00000000-0000-0001-0000-000000000001	4	2014-06-11 09:52:05.05347	574913855	2014-06-11 09:52:05.05	DELETED	71883
196	91	00000000-0000-0001-0000-000000000001	3	2014-06-13 15:38:58.383637	574913855	2014-06-13 15:38:58.38	DELETED	71883
197	94	00000000-0000-0001-0000-000000000001	1	2014-06-13 15:53:39.714179	574913855	2014-06-13 15:53:39.697	NEW	71883
198	81	00000000-0000-0001-0000-000000000001	5	2014-06-13 16:00:07.666645	574913855	2014-06-13 16:00:07.663	DELETED	71883
199	95	00000000-0000-0001-0000-000000000001	1	2014-06-13 16:00:23.218212	0	2014-06-13 16:00:23.214	NEW	0
200	96	00000000-0000-0001-0000-000000000001	1	2014-06-13 16:00:25.085291	574913855	2014-06-13 16:00:25.076	NEW	71883
201	97	00000000-0000-0001-0000-000000000001	1	2014-06-13 16:00:26.8786	252178708	2014-06-13 16:00:26.869	NEW	94338
202	75	00000000-0000-0001-0000-000000000001	3	2014-06-13 18:09:22.353286	574913855	2014-06-06 11:47:54.069	RENAMED	71883
203	94	00000000-0000-0001-0000-000000000001	2	2014-06-13 18:14:21.459901	574913855	2014-06-13 15:53:39.697	RENAMED	71883
204	75	00000000-0000-0001-0000-000000000001	4	2014-06-13 18:15:01.263562	574913855	2014-06-13 18:15:01.26	DELETED	71883
205	98	00000000-0000-0001-0000-000000000001	1	2014-06-13 18:16:50.624712	574913855	2014-06-13 18:16:50.608	NEW	71883
206	98	00000000-0000-0001-0000-000000000001	2	2014-06-13 18:17:08.65844	574913855	2014-06-13 18:16:50.608	RENAMED	71883
207	77	00000000-0000-0001-0000-000000000001	2	2014-06-13 18:20:33.43787	574913855	2014-06-06 11:58:19.966	RENAMED	71883
208	76	00000000-0000-0001-0000-000000000001	4	2014-06-13 18:23:02.649931	574913855	2014-06-06 11:51:17.376	RENAMED	71883
209	54	00000000-0000-0001-0000-000000000001	2	2014-06-16 09:57:19.92693	0	2014-06-05 16:00:30.245	RENAMED	0
210	54	00000000-0000-0001-0000-000000000001	3	2014-06-16 16:17:31.187849	0	2014-06-16 16:17:31.184	DELETED	0
211	55	00000000-0000-0001-0000-000000000001	2	2014-06-16 16:17:31.211017	252178708	2014-06-16 16:17:31.184	DELETED	94338
212	56	00000000-0000-0001-0000-000000000001	2	2014-06-16 16:17:31.235959	574913855	2014-06-16 16:17:31.184	DELETED	71883
213	95	00000000-0000-0001-0000-000000000001	2	2014-06-16 16:20:27.067572	0	2014-06-13 16:00:23.214	RENAMED	0
214	99	00000000-0000-0001-0000-000000000001	1	2014-06-16 16:21:27.651088	0	2014-06-16 16:21:27.648	NEW	0
215	100	00000000-0000-0001-0000-000000000001	1	2014-06-16 16:21:29.235655	252178708	2014-06-16 16:21:29.226	NEW	94338
216	101	00000000-0000-0001-0000-000000000001	1	2014-06-16 16:21:30.660027	574913855	2014-06-16 16:21:30.65	NEW	71883
217	99	00000000-0000-0001-0000-000000000001	2	2014-06-16 16:26:46.639598	0	2014-06-16 16:21:27.648	RENAMED	0
218	99	00000000-0000-0001-0000-000000000001	3	2014-06-16 16:29:21.398376	0	2014-06-16 16:21:27.648	RENAMED	0
219	102	00000000-0000-0001-0000-000000000001	1	2014-06-16 16:32:14.3065	0	2014-06-16 16:32:14.303	NEW	0
220	103	00000000-0000-0001-0000-000000000001	1	2014-06-16 16:32:15.724673	252178708	2014-06-16 16:32:15.715	NEW	94338
221	104	00000000-0000-0001-0000-000000000001	1	2014-06-16 16:32:17.00816	574913855	2014-06-16 16:32:16.998	NEW	71883
222	102	00000000-0000-0001-0000-000000000001	2	2014-06-16 16:33:57.629604	0	2014-06-16 16:32:14.303	RENAMED	0
223	80	00000000-0000-0001-0000-000000000001	4	2014-06-16 16:40:58.971258	574913855	2014-06-06 12:01:41.876	RENAMED	71883
224	105	00000000-0000-0001-0000-000000000001	1	2014-06-16 16:41:38.041696	0	2014-06-16 16:41:38.038	NEW	0
225	106	00000000-0000-0001-0000-000000000001	1	2014-06-16 16:41:40.292034	252178708	2014-06-16 16:41:40.283	NEW	94338
226	107	00000000-0000-0001-0000-000000000001	1	2014-06-16 16:41:42.599852	574913855	2014-06-16 16:41:42.59	NEW	71883
227	102	00000000-0000-0001-0000-000000000001	3	2014-06-16 16:42:13.322837	0	2014-06-16 16:42:13.32	DELETED	0
228	103	00000000-0000-0001-0000-000000000001	2	2014-06-16 16:42:13.349014	252178708	2014-06-16 16:42:13.32	DELETED	94338
229	104	00000000-0000-0001-0000-000000000001	2	2014-06-16 16:42:13.362914	574913855	2014-06-16 16:42:13.32	DELETED	71883
230	105	00000000-0000-0001-0000-000000000001	2	2014-06-16 16:42:25.538664	0	2014-06-16 16:41:38.038	RENAMED	0
231	78	00000000-0000-0001-0000-000000000001	5	2014-06-16 16:59:34.179259	574913855	2014-06-06 11:59:04.783	RENAMED	71883
232	3	00000000-0000-0001-0000-000000000001	2	2014-06-16 17:11:10.248453	2190678904	2014-05-28 15:29:21.807	RENAMED	78
233	108	00000000-0000-0001-0000-000000000001	1	2014-06-16 17:14:07.572283	0	2014-06-16 17:14:07.568	NEW	0
234	108	00000000-0000-0001-0000-000000000001	2	2014-06-16 17:14:13.808087	0	2014-06-16 17:14:07.568	RENAMED	0
235	82	00000000-0000-0001-0000-000000000001	2	2014-06-16 17:14:19.734144	1480984607	2014-06-06 12:14:04.552	RENAMED	20
236	2	00000000-0000-0001-0000-000000000001	2	2014-06-16 17:19:25.12299	2190678904	2014-05-28 14:13:36.617	RENAMED	78
237	78	00000000-0000-0001-0000-000000000001	6	2014-06-16 17:19:26.187329	574913855	2014-06-06 11:59:04.783	RENAMED	71883
238	80	00000000-0000-0001-0000-000000000001	5	2014-06-16 17:26:29.956816	574913855	2014-06-06 12:01:41.876	RENAMED	71883
239	43	00000000-0000-0001-0000-000000000001	4	2014-06-16 17:26:30.875215	0	2014-06-05 10:22:40.099	RENAMED	0
240	82	00000000-0000-0001-0000-000000000001	3	2014-06-16 17:27:38.265949	1480984607	2014-06-06 12:14:04.552	RENAMED	20
241	2	00000000-0000-0001-0000-000000000001	3	2014-06-16 17:27:39.211559	2190678904	2014-05-28 14:13:36.617	RENAMED	78
242	80	00000000-0000-0001-0000-000000000001	6	2014-06-16 17:27:40.207606	574913855	2014-06-06 12:01:41.876	RENAMED	71883
243	43	00000000-0000-0001-0000-000000000001	5	2014-06-16 17:27:41.155127	0	2014-06-05 10:22:40.099	RENAMED	0
244	108	00000000-0000-0001-0000-000000000001	3	2014-06-16 17:27:57.477941	0	2014-06-16 17:27:57.475	DELETED	0
245	3	00000000-0000-0001-0000-000000000001	3	2014-06-16 17:28:08.417026	2190678904	2014-05-28 15:29:21.807	RENAMED	78
246	1	00000000-0000-0001-0000-000000000001	3	2014-06-16 18:31:26.887576	0	2014-06-16 18:31:26.885	DELETED	0
247	2	00000000-0000-0001-0000-000000000001	4	2014-06-16 18:31:26.90522	2190678904	2014-06-16 18:31:26.885	DELETED	78
248	3	00000000-0000-0001-0000-000000000001	4	2014-06-16 18:31:26.922046	2190678904	2014-06-16 18:31:26.885	DELETED	78
249	43	00000000-0000-0001-0000-000000000001	6	2014-06-16 18:31:26.938775	0	2014-06-16 18:31:26.885	DELETED	0
250	44	00000000-0000-0001-0000-000000000001	2	2014-06-16 18:31:26.95542	75629005	2014-06-16 18:31:26.885	DELETED	4
251	57	00000000-0000-0001-0000-000000000001	2	2014-06-16 18:31:26.972035	0	2014-06-16 18:31:26.885	DELETED	0
252	58	00000000-0000-0001-0000-000000000001	2	2014-06-16 18:31:26.988702	574913855	2014-06-16 18:31:26.885	DELETED	71883
253	59	00000000-0000-0001-0000-000000000001	2	2014-06-16 18:31:27.005148	252178708	2014-06-16 18:31:26.885	DELETED	94338
254	83	00000000-0000-0001-0000-000000000001	5	2014-06-16 18:31:27.038644	95683046	2014-06-16 18:31:26.885	DELETED	5
255	84	00000000-0000-0001-0000-000000000001	3	2014-06-16 18:31:27.047883	1981548946	2014-06-16 18:31:26.885	DELETED	24
256	105	00000000-0000-0001-0000-000000000001	3	2014-06-16 18:31:27.063478	0	2014-06-16 18:31:26.885	DELETED	0
257	106	00000000-0000-0001-0000-000000000001	2	2014-06-16 18:31:27.072513	252178708	2014-06-16 18:31:26.885	DELETED	94338
258	107	00000000-0000-0001-0000-000000000001	2	2014-06-16 18:31:27.081093	574913855	2014-06-16 18:31:26.885	DELETED	71883
259	80	00000000-0000-0001-0000-000000000001	7	2014-06-16 18:31:27.0894	574913855	2014-06-16 18:31:26.885	DELETED	71883
260	82	00000000-0000-0001-0000-000000000001	4	2014-06-16 18:31:27.105222	1480984607	2014-06-16 18:31:26.885	DELETED	20
261	109	00000000-0000-0001-0000-000000000001	1	2014-06-16 18:35:17.015826	0	2014-06-16 18:35:17.013	NEW	0
262	110	00000000-0000-0001-0000-000000000001	1	2014-06-16 18:35:18.450828	252178708	2014-06-16 18:35:18.443	NEW	94338
263	111	00000000-0000-0001-0000-000000000001	1	2014-06-16 18:35:19.734378	574913855	2014-06-16 18:35:19.726	NEW	71883
264	109	00000000-0000-0001-0000-000000000001	2	2014-06-16 18:37:28.445652	0	2014-06-16 18:37:28.443	DELETED	0
265	110	00000000-0000-0001-0000-000000000001	2	2014-06-16 18:37:28.457736	252178708	2014-06-16 18:37:28.443	DELETED	94338
266	111	00000000-0000-0001-0000-000000000001	2	2014-06-16 18:37:28.481832	574913855	2014-06-16 18:37:28.443	DELETED	71883
267	112	00000000-0000-0001-0000-000000000001	1	2014-06-16 18:38:44.83747	0	2014-06-16 18:38:44.835	NEW	0
268	113	00000000-0000-0001-0000-000000000001	1	2014-06-16 18:38:46.081991	252178708	2014-06-16 18:38:46.074	NEW	94338
269	114	00000000-0000-0001-0000-000000000001	1	2014-06-16 18:38:47.394234	574913855	2014-06-16 18:38:47.388	NEW	71883
270	112	00000000-0000-0001-0000-000000000001	2	2014-06-16 18:39:28.84197	0	2014-06-16 18:39:28.839	DELETED	0
271	113	00000000-0000-0001-0000-000000000001	2	2014-06-16 18:39:28.856842	252178708	2014-06-16 18:39:28.839	DELETED	94338
272	114	00000000-0000-0001-0000-000000000001	2	2014-06-16 18:39:28.869941	574913855	2014-06-16 18:39:28.839	DELETED	71883
273	93	00000000-0000-0001-0000-000000000001	3	2014-06-16 18:39:29.487457	113377856	2014-06-16 18:39:29.485	DELETED	5
274	115	00000000-0000-0001-0000-000000000001	1	2014-06-16 18:39:45.658921	113377856	2014-06-16 18:39:45.648	NEW	5
275	116	00000000-0000-0001-0000-000000000001	1	2014-06-16 18:39:46.539968	0	2014-06-16 18:39:46.537	NEW	0
276	117	00000000-0000-0001-0000-000000000001	1	2014-06-16 18:39:47.912507	252178708	2014-06-16 18:39:47.904	NEW	94338
277	118	00000000-0000-0001-0000-000000000001	1	2014-06-16 18:39:49.195444	574913855	2014-06-16 18:39:49.187	NEW	71883
278	119	00000000-0000-0001-0000-000000000001	1	2014-06-17 13:21:06.260681	2260022444	2014-06-17 13:21:06.247	NEW	325
279	119	00000000-0000-0001-0000-000000000001	2	2014-06-17 13:32:17.448214	586247373	2014-06-17 13:21:06.247	CHANGED	548
280	115	00000000-0000-0001-0000-000000000001	2	2014-06-17 13:43:54.223522	264766307	2014-06-16 18:39:45.648	CHANGED	8
281	120	00000000-0000-0001-0000-000000000001	1	2014-06-17 16:49:24.319558	3347581786	2014-06-17 16:49:24.307	NEW	32
282	121	00000000-0000-0001-0000-000000000001	1	2014-06-17 16:50:37.260264	1825835166	2014-06-17 16:50:37.251	NEW	26
283	121	00000000-0000-0001-0000-000000000001	2	2014-06-17 16:50:58.738588	1825835166	2014-06-17 16:50:37.251	RENAMED	26
284	121	00000000-0000-0001-0000-000000000001	3	2014-06-17 16:51:21.117169	29884645	2014-06-17 16:50:37.251	CHANGED	3
285	121	00000000-0000-0001-0000-000000000001	4	2014-06-17 16:51:37.377228	2554399192	2014-06-17 16:50:37.251	CHANGED	28
286	121	00000000-0000-0001-0000-000000000001	5	2014-06-17 17:12:23.485191	2554399192	2014-06-17 16:50:37.251	RENAMED	28
287	121	00000000-0000-0001-0000-000000000001	6	2014-06-17 17:12:53.799732	2554399192	2014-06-17 16:50:37.251	RENAMED	28
288	94	00000000-0000-0001-0000-000000000001	3	2014-06-17 17:13:18.455345	574913855	2014-06-13 15:53:39.697	RENAMED	71883
289	122	00000000-0000-0001-0000-000000000001	1	2014-06-17 17:13:35.772457	574913855	2014-06-17 17:13:35.76	NEW	71883
290	123	00000000-0000-0001-0000-000000000001	1	2014-06-17 17:18:13.763941	0	2014-06-17 17:18:13.761	NEW	0
291	124	00000000-0000-0001-0000-000000000001	1	2014-06-17 17:18:15.181477	574913855	2014-06-17 17:18:15.173	NEW	71883
292	125	00000000-0000-0001-0000-000000000001	1	2014-06-17 17:18:16.756674	252178708	2014-06-17 17:18:16.748	NEW	94338
293	126	00000000-0000-0001-0000-000000000001	1	2014-06-17 17:18:26.761886	0	2014-06-17 17:18:26.759	NEW	0
294	126	00000000-0000-0001-0000-000000000001	2	2014-06-17 17:18:37.716021	0	2014-06-17 17:18:26.759	RENAMED	0
295	127	00000000-0000-0001-0000-000000000001	1	2014-06-17 17:19:00.459406	2554399192	2014-06-17 17:19:00.452	NEW	28
296	128	00000000-0000-0001-0000-000000000001	1	2014-06-17 17:19:01.279268	0	2014-06-17 17:19:01.276	NEW	0
297	129	00000000-0000-0001-0000-000000000001	1	2014-06-17 17:19:02.850182	252178708	2014-06-17 17:19:02.843	NEW	94338
298	130	00000000-0000-0001-0000-000000000001	1	2014-06-17 17:19:04.392671	574913855	2014-06-17 17:19:04.384	NEW	71883
299	121	00000000-0000-0001-0000-000000000001	7	2014-06-17 17:19:26.332438	2554399192	2014-06-17 16:50:37.251	RENAMED	28
300	123	00000000-0000-0001-0000-000000000001	2	2014-06-17 17:19:26.953455	0	2014-06-17 17:18:13.761	RENAMED	0
301	121	00000000-0000-0001-0000-000000000001	8	2014-06-17 17:19:43.47069	2554399192	2014-06-17 16:50:37.251	RENAMED	28
302	123	00000000-0000-0001-0000-000000000001	3	2014-06-17 17:19:44.086089	0	2014-06-17 17:18:13.761	RENAMED	0
303	123	00000000-0000-0001-0000-000000000001	4	2014-06-17 17:20:21.056836	0	2014-06-17 17:20:21.054	DELETED	0
304	124	00000000-0000-0001-0000-000000000001	2	2014-06-17 17:20:21.085556	574913855	2014-06-17 17:20:21.054	DELETED	71883
305	125	00000000-0000-0001-0000-000000000001	2	2014-06-17 17:20:21.098884	252178708	2014-06-17 17:20:21.054	DELETED	94338
306	121	00000000-0000-0001-0000-000000000001	9	2014-06-17 17:20:21.48863	2554399192	2014-06-17 17:20:21.486	DELETED	28
307	127	00000000-0000-0001-0000-000000000001	2	2014-06-17 17:20:45.688593	2554399192	2014-06-17 17:19:00.452	RENAMED	28
308	94	00000000-0000-0001-0000-000000000001	4	2014-06-19 10:05:17.747421	574913855	2014-06-19 10:05:17.745	DELETED	71883
309	128	00000000-0000-0001-0000-000000000001	2	2014-06-19 10:05:38.989504	0	2014-06-17 17:19:01.276	RENAMED	0
310	120	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:14:24.582045	1825838054	2014-06-17 16:49:24.307	CHANGED	59
311	120	00000000-0000-0001-0000-000000000001	3	2014-06-19 15:15:09.779102	1825838054	2014-06-17 16:49:24.307	RENAMED	59
312	131	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:20:25.522916	869009016	2014-06-19 15:20:25.516	NEW	16
313	131	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:23:12.257696	869009016	2014-06-19 15:23:12.255	DELETED	16
314	132	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:23:57.505734	0	2014-06-19 15:23:57.502	NEW	0
315	132	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:24:14.831341	0	2014-06-19 15:23:57.502	RENAMED	0
316	132	00000000-0000-0001-0000-000000000001	3	2014-06-19 15:24:42.973711	0	2014-06-19 15:24:42.971	DELETED	0
317	133	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:44:17.462191	586247373	2014-06-19 15:44:17.454	NEW	548
318	134	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:44:21.186301	574913855	2014-06-19 15:44:21.179	NEW	71883
319	135	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:44:22.752004	0	2014-06-19 15:44:22.749	NEW	0
320	136	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:44:24.168352	0	2014-06-19 15:44:24.165	NEW	0
321	137	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:44:26.576824	574913855	2014-06-19 15:44:26.569	NEW	71883
322	138	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:44:28.926828	252178708	2014-06-19 15:44:28.919	NEW	94338
323	139	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:44:30.925878	574913855	2014-06-19 15:44:30.918	NEW	71883
324	140	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:44:32.725789	574913855	2014-06-19 15:44:32.718	NEW	71883
325	141	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:44:34.932247	574913855	2014-06-19 15:44:34.926	NEW	71883
326	135	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:45:35.343626	0	2014-06-19 15:45:35.341	DELETED	0
327	136	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:45:35.357638	0	2014-06-19 15:45:35.341	DELETED	0
328	137	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:45:35.372025	574913855	2014-06-19 15:45:35.341	DELETED	71883
329	138	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:45:35.382362	252178708	2014-06-19 15:45:35.341	DELETED	94338
330	139	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:45:35.390695	574913855	2014-06-19 15:45:35.341	DELETED	71883
331	140	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:45:35.406834	574913855	2014-06-19 15:45:35.341	DELETED	71883
332	141	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:45:35.42169	574913855	2014-06-19 15:45:35.341	DELETED	71883
333	134	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:45:35.946112	574913855	2014-06-19 15:45:35.943	DELETED	71883
334	133	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:45:36.437949	586247373	2014-06-19 15:45:36.435	DELETED	548
335	142	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:47:22.127585	586247373	2014-06-19 15:47:22.12	NEW	548
336	143	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:47:23.720828	574913855	2014-06-19 15:47:23.713	NEW	71883
337	144	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:47:24.708205	0	2014-06-19 15:47:24.705	NEW	0
338	145	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:47:25.618121	0	2014-06-19 15:47:25.615	NEW	0
339	146	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:47:27.160722	574913855	2014-06-19 15:47:27.153	NEW	71883
340	147	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:47:28.794259	252178708	2014-06-19 15:47:28.787	NEW	94338
341	148	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:47:30.392819	574913855	2014-06-19 15:47:30.385	NEW	71883
342	149	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:47:31.969583	574913855	2014-06-19 15:47:31.962	NEW	71883
343	150	00000000-0000-0001-0000-000000000001	1	2014-06-19 15:47:33.643789	574913855	2014-06-19 15:47:33.636	NEW	71883
344	115	00000000-0000-0001-0000-000000000001	3	2014-06-19 15:48:37.072055	264766307	2014-06-16 18:39:45.648	RENAMED	8
345	52	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:48:38.126184	574913855	2014-06-05 15:12:00.054	RENAMED	71883
346	78	00000000-0000-0001-0000-000000000001	7	2014-06-19 15:48:39.226501	574913855	2014-06-06 11:59:04.783	RENAMED	71883
347	127	00000000-0000-0001-0000-000000000001	3	2014-06-19 15:48:40.37545	2554399192	2014-06-17 17:19:00.452	RENAMED	28
348	120	00000000-0000-0001-0000-000000000001	4	2014-06-19 15:48:41.490819	1825838054	2014-06-17 16:49:24.307	RENAMED	59
349	122	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:49:49.255443	574913855	2014-06-19 15:49:49.253	DELETED	71883
350	119	00000000-0000-0001-0000-000000000001	3	2014-06-19 15:49:55.698136	586247373	2014-06-19 15:49:55.696	DELETED	548
351	115	00000000-0000-0001-0000-000000000001	4	2014-06-19 15:50:27.384147	264766307	2014-06-19 15:50:27.382	DELETED	8
352	142	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:50:35.202218	586247373	2014-06-19 15:50:35.2	DELETED	548
353	143	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:50:44.550457	574913855	2014-06-19 15:50:44.548	DELETED	71883
354	78	00000000-0000-0001-0000-000000000001	8	2014-06-19 15:50:45.08691	574913855	2014-06-19 15:50:45.084	DELETED	71883
355	52	00000000-0000-0001-0000-000000000001	3	2014-06-19 15:50:45.609721	574913855	2014-06-19 15:50:45.607	DELETED	71883
356	127	00000000-0000-0001-0000-000000000001	4	2014-06-19 15:51:00.200375	2554399192	2014-06-19 15:51:00.198	DELETED	28
357	148	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:54:08.20528	574913855	2014-06-19 15:54:08.203	DELETED	71883
358	150	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:54:08.789866	574913855	2014-06-19 15:54:08.787	DELETED	71883
359	149	00000000-0000-0001-0000-000000000001	2	2014-06-19 15:54:17.025809	574913855	2014-06-19 15:54:17.023	DELETED	71883
360	151	00000000-0000-0001-0000-000000000001	1	2014-06-19 16:55:42.437203	415216009	2014-06-19 16:55:42.43	NEW	13316
361	151	00000000-0000-0001-0000-000000000001	2	2014-06-19 17:00:34.22945	415216009	2014-06-19 17:00:34.227	DELETED	13316
362	152	00000000-0000-0001-0000-000000000001	1	2014-06-20 10:06:55.524257	433521783	2014-06-20 10:06:55.515	NEW	11
363	153	00000000-0000-0001-0000-000000000001	1	2014-06-20 10:08:05.724655	415216009	2014-06-20 10:08:05.715	NEW	13316
364	152	00000000-0000-0001-0000-000000000001	2	2014-06-20 10:08:55.953424	433521783	2014-06-20 10:06:55.515	RENAMED	11
365	152	00000000-0000-0001-0000-000000000001	3	2014-06-20 10:09:14.768952	433521783	2014-06-20 10:09:14.767	DELETED	11
366	154	00000000-0000-0001-0000-000000000001	1	2014-06-20 10:10:13.802333	0	2014-06-20 10:10:13.799	NEW	0
367	154	00000000-0000-0001-0000-000000000001	2	2014-06-20 10:10:27.475739	0	2014-06-20 10:10:13.799	RENAMED	0
368	154	00000000-0000-0001-0000-000000000001	3	2014-06-20 10:10:43.637053	0	2014-06-20 10:10:43.635	DELETED	0
369	155	00000000-0000-0001-0000-000000000001	1	2014-06-20 10:11:11.039534	1825838054	2014-06-20 10:11:11.031	NEW	59
370	156	00000000-0000-0001-0000-000000000001	1	2014-06-20 10:11:11.837452	0	2014-06-20 10:11:11.834	NEW	0
371	157	00000000-0000-0001-0000-000000000001	1	2014-06-20 10:11:12.616525	0	2014-06-20 10:11:12.614	NEW	0
372	158	00000000-0000-0001-0000-000000000001	1	2014-06-20 10:11:14.177735	252178708	2014-06-20 10:11:14.17	NEW	94338
373	159	00000000-0000-0001-0000-000000000001	1	2014-06-20 10:11:15.728073	574913855	2014-06-20 10:11:15.721	NEW	71883
374	155	00000000-0000-0001-0000-000000000001	2	2014-06-20 10:14:51.826635	1825838054	2014-06-20 10:11:11.031	RENAMED	59
375	156	00000000-0000-0001-0000-000000000001	2	2014-06-20 10:14:52.649411	0	2014-06-20 10:11:11.834	RENAMED	0
376	160	00000000-0000-0001-0000-000000000001	1	2014-06-20 11:43:49.453493	0	2014-06-20 11:43:49.451	NEW	0
377	161	00000000-0000-0001-0000-000000000001	1	2014-06-20 12:35:05.785835	814358719	2014-06-20 12:35:05.777	NEW	4024
378	161	00000000-0000-0001-0000-000000000001	2	2014-06-20 12:45:24.426897	814358719	2014-06-20 12:45:24.425	DELETED	4024
379	162	00000000-0000-0001-0000-000000000001	1	2014-06-20 13:07:54.596627	567223821	2014-06-20 13:07:54.588	NEW	116
380	163	00000000-0000-0001-0000-000000000001	1	2014-06-20 13:08:30.050145	567223821	2014-06-20 13:08:30.043	NEW	116
381	164	00000000-0000-0001-0000-000000000001	1	2014-06-20 14:09:19.345762	567223821	2014-06-20 14:09:19.338	NEW	116
382	165	00000000-0000-0001-0000-000000000001	1	2014-06-20 14:21:39.309013	3720630253	2014-06-20 14:21:39.3	NEW	387740
383	166	00000000-0000-0001-0000-000000000001	1	2014-06-20 14:22:52.302392	1780681089	2014-06-20 14:22:52.295	NEW	1150940
384	167	00000000-0000-0001-0000-000000000001	1	2014-06-20 14:26:15.944858	1162315682	2014-06-20 14:26:15.937	NEW	693716
385	167	00000000-0000-0001-0000-000000000001	2	2014-06-20 14:26:28.927866	1162315682	2014-06-20 14:26:28.925	DELETED	693716
386	168	00000000-0000-0001-0000-000000000001	1	2014-06-20 14:26:49.764703	0	2014-06-20 14:26:49.762	NEW	0
387	126	00000000-0000-0001-0000-000000000001	3	2014-06-20 16:30:37.05795	0	2014-06-17 17:18:26.759	RENAMED	0
388	126	00000000-0000-0001-0000-000000000001	4	2014-06-20 16:31:08.900955	0	2014-06-17 17:18:26.759	RENAMED	0
389	168	00000000-0000-0001-0000-000000000001	2	2014-06-20 16:43:15.721714	0	2014-06-20 14:26:49.762	RENAMED	0
390	162	00000000-0000-0001-0000-000000000001	2	2014-06-20 16:44:45.854386	567223821	2014-06-20 13:07:54.588	RENAMED	116
391	162	00000000-0000-0001-0000-000000000001	3	2014-06-20 16:47:07.763031	567223821	2014-06-20 13:07:54.588	RENAMED	116
392	162	00000000-0000-0001-0000-000000000001	4	2014-06-20 16:48:16.632642	567223821	2014-06-20 13:07:54.588	RENAMED	116
393	168	00000000-0000-0001-0000-000000000001	3	2014-06-20 17:05:05.233392	0	2014-06-20 14:26:49.762	RENAMED	0
394	168	00000000-0000-0001-0000-000000000001	4	2014-06-20 17:09:11.050904	0	2014-06-20 14:26:49.762	RENAMED	0
395	160	deb3e598-0df9-44ae-b2a6-1ef28ff88ec2	2	2014-06-20 17:09:35.74397	0	2014-06-20 17:08:12	RENAMED	0
396	163	deb3e598-0df9-44ae-b2a6-1ef28ff88ec2	2	2014-06-20 17:09:50.727508	567223821	2014-06-20 13:08:30	RENAMED	116
397	160	deb3e598-0df9-44ae-b2a6-1ef28ff88ec2	3	2014-06-20 17:10:00.7304	0	2014-06-20 17:08:12	RENAMED	0
398	163	deb3e598-0df9-44ae-b2a6-1ef28ff88ec2	3	2014-06-20 17:10:00.751061	567223821	2014-06-20 13:08:30	RENAMED	116
399	169	deb3e598-0df9-44ae-b2a6-1ef28ff88ec2	1	2014-06-20 17:10:20.724552	1665495150	2013-03-04 10:50:08	NEW	525662
400	170	deb3e598-0df9-44ae-b2a6-1ef28ff88ec2	1	2014-06-20 17:10:45.731449	1	2014-06-20 17:10:17	NEW	0
401	170	deb3e598-0df9-44ae-b2a6-1ef28ff88ec2	2	2014-06-20 17:10:45.747394	1	2014-06-20 17:10:17	RENAMED	0
402	170	deb3e598-0df9-44ae-b2a6-1ef28ff88ec2	3	2014-06-20 17:11:00.708867	306774877	2014-06-20 17:10:37	CHANGED	9
403	170	deb3e598-0df9-44ae-b2a6-1ef28ff88ec2	4	2014-06-20 17:11:40.729479	2234059191	2014-06-20 17:11:15	CHANGED	26
404	162	00000000-0000-0001-0000-000000000001	5	2014-06-25 00:39:48.271644	567223821	2014-06-25 00:39:48.269	DELETED	116
405	163	00000000-0000-0001-0000-000000000001	4	2014-06-25 00:40:04.539948	567223821	2014-06-25 00:40:04.537	DELETED	116
406	160	b7be802e-a8ee-40b2-9cfa-bbb2c4d0da81	4	2014-06-25 00:40:29.715732	0	2014-06-20 17:08:12	DELETED	0
407	171	00000000-0000-0001-0000-000000000001	1	2014-06-25 00:40:49.995658	0	2014-06-25 00:40:49.993	NEW	0
408	172	00000000-0000-0001-0000-000000000001	1	2014-06-26 02:33:20.02666	755041787	2014-06-26 02:33:20.016	NEW	14
409	173	00000000-0000-0001-0000-000000000001	1	2014-06-26 02:34:36.508975	2051169444	2014-06-26 02:34:36.498	NEW	1577171
410	173	00000000-0000-0001-0000-000000000001	2	2014-06-26 02:35:29.271347	2051169444	2014-06-26 02:35:29.269	DELETED	1577171
411	174	00000000-0000-0001-0000-000000000001	1	2014-06-26 02:36:07.124217	670659562	2014-06-26 02:36:07.113	NEW	80504
412	174	00000000-0000-0001-0000-000000000001	2	2014-06-26 02:36:42.080913	670659562	2014-06-26 02:36:42.078	DELETED	80504
413	175	00000000-0000-0001-0000-000000000001	1	2014-06-26 02:37:02.543713	0	2014-06-26 02:37:02.541	NEW	0
414	175	00000000-0000-0001-0000-000000000001	2	2014-06-26 02:37:21.087464	0	2014-06-26 02:37:02.541	RENAMED	0
415	175	00000000-0000-0001-0000-000000000001	3	2014-06-26 02:37:53.865255	0	2014-06-26 02:37:53.863	DELETED	0
416	176	00000000-0000-0001-0000-000000000001	1	2014-06-26 02:38:35.767096	574913855	2014-06-26 02:38:35.756	NEW	71883
417	177	00000000-0000-0001-0000-000000000001	1	2014-06-26 02:38:37.384368	574913855	2014-06-26 02:38:37.373	NEW	71883
418	176	00000000-0000-0001-0000-000000000001	2	2014-06-26 02:39:32.688612	574913855	2014-06-26 02:38:35.756	RENAMED	71883
419	177	00000000-0000-0001-0000-000000000001	2	2014-06-26 02:39:33.662677	574913855	2014-06-26 02:38:37.373	RENAMED	71883
420	156	00000000-0000-0001-0000-000000000001	3	2014-06-26 03:13:57.24171	0	2014-06-26 03:13:57.239	DELETED	0
421	157	00000000-0000-0001-0000-000000000001	2	2014-06-26 03:13:57.250952	0	2014-06-26 03:13:57.239	DELETED	0
422	158	00000000-0000-0001-0000-000000000001	2	2014-06-26 03:13:57.259245	252178708	2014-06-26 03:13:57.239	DELETED	94338
423	159	00000000-0000-0001-0000-000000000001	2	2014-06-26 03:13:57.2675	574913855	2014-06-26 03:13:57.239	DELETED	71883
424	172	00000000-0000-0001-0000-000000000001	2	2014-06-26 03:13:57.662891	755041787	2014-06-26 03:13:57.66	DELETED	14
425	155	00000000-0000-0001-0000-000000000001	3	2014-06-26 03:14:17.966345	1825838054	2014-06-20 10:11:11.031	RENAMED	59
426	98	00000000-0000-0001-0000-000000000001	3	2014-06-26 03:14:41.821419	574913855	2014-06-26 03:14:41.819	DELETED	71883
427	77	00000000-0000-0001-0000-000000000001	3	2014-06-26 03:14:49.047691	574913855	2014-06-26 03:14:49.045	DELETED	71883
428	120	00000000-0000-0001-0000-000000000001	5	2014-06-26 03:15:02.483095	1825838054	2014-06-26 03:15:02.48	DELETED	59
429	126	00000000-0000-0001-0000-000000000001	5	2014-06-26 03:15:14.914505	0	2014-06-26 03:15:14.912	DELETED	0
430	144	00000000-0000-0001-0000-000000000001	2	2014-06-26 03:15:14.930797	0	2014-06-26 03:15:14.912	DELETED	0
431	145	00000000-0000-0001-0000-000000000001	2	2014-06-26 03:15:14.947292	0	2014-06-26 03:15:14.912	DELETED	0
432	146	00000000-0000-0001-0000-000000000001	2	2014-06-26 03:15:14.96387	574913855	2014-06-26 03:15:14.912	DELETED	71883
433	147	00000000-0000-0001-0000-000000000001	2	2014-06-26 03:15:14.980434	252178708	2014-06-26 03:15:14.912	DELETED	94338
434	176	00000000-0000-0001-0000-000000000001	3	2014-06-26 03:15:14.997129	574913855	2014-06-26 03:15:14.912	DELETED	71883
435	177	00000000-0000-0001-0000-000000000001	3	2014-06-26 03:15:15.006204	574913855	2014-06-26 03:15:14.912	DELETED	71883
436	178	00000000-0000-0001-0000-000000000001	1	2014-06-26 09:31:18.597442	0	2014-06-26 09:31:18.595	NEW	0
437	178	28f2ed9a-f840-475b-a29f-a71fdcb41ac8	2	2014-06-26 09:31:42.941977	0	2014-06-26 09:31:18.595	DELETED	0
438	179	00000000-0000-0001-0000-000000000001	1	2014-06-26 11:52:58.261956	310510519	2014-06-26 11:52:58.251	NEW	9
439	180	00000000-0000-0001-0000-000000000001	1	2014-06-26 11:58:48.734153	1887357546	2014-06-26 11:58:48.725	NEW	1138318
440	181	00000000-0000-0001-0000-000000000001	1	2014-06-26 11:59:23.707001	0	2014-06-26 11:59:23.704	NEW	0
441	182	00000000-0000-0001-0000-000000000001	1	2014-06-26 11:59:36.418319	1887357546	2014-06-26 11:59:36.411	NEW	1138318
442	183	00000000-0000-0001-0000-000000000001	1	2014-06-26 11:59:55.591568	1517059482	2014-06-26 11:59:55.584	NEW	175196
443	184	00000000-0000-0001-0000-000000000001	1	2014-06-26 12:48:59.959572	0	2014-06-26 12:48:59.956	NEW	0
444	184	00000000-0000-0001-0000-000000000001	2	2014-06-26 12:49:07.654218	0	2014-06-26 12:48:59.956	RENAMED	0
445	185	00000000-0000-0001-0000-000000000001	1	2014-06-26 12:49:25.951332	75629005	2014-06-26 12:49:25.944	NEW	4
446	186	00000000-0000-0001-0000-000000000001	1	2014-06-26 12:49:41.430792	95683046	2014-06-26 12:49:41.423	NEW	5
447	185	00000000-0000-0001-0000-000000000001	2	2014-06-26 12:50:01.444613	75629005	2014-06-26 12:50:01.442	DELETED	4
448	186	00000000-0000-0001-0000-000000000001	2	2014-06-26 12:50:01.83071	95683046	2014-06-26 12:50:01.828	DELETED	5
449	187	00000000-0000-0001-0000-000000000001	1	2014-06-26 12:51:17.631945	38338852	2014-06-26 12:51:17.624	NEW	3
450	188	00000000-0000-0001-0000-000000000001	1	2014-06-26 12:51:30.478964	135266893	2014-06-26 12:51:30.472	NEW	6
451	187	00000000-0000-0001-0000-000000000001	2	2014-06-26 12:51:41.21515	38338852	2014-06-26 12:51:41.213	DELETED	3
452	188	00000000-0000-0001-0000-000000000001	2	2014-06-26 12:51:41.575506	135266893	2014-06-26 12:51:41.573	DELETED	6
453	189	00000000-0000-0001-0000-000000000001	1	2014-06-26 12:52:43.339342	38338852	2014-06-26 12:52:43.332	NEW	3
454	190	00000000-0000-0001-0000-000000000001	1	2014-06-26 12:52:54.877488	235275062	2014-06-26 12:52:54.87	NEW	8
455	189	00000000-0000-0001-0000-000000000001	2	2014-06-26 12:53:02.260708	38338852	2014-06-26 12:53:02.258	DELETED	3
456	190	00000000-0000-0001-0000-000000000001	2	2014-06-26 12:53:02.65066	235275062	2014-06-26 12:53:02.648	DELETED	8
457	184	00000000-0000-0001-0000-000000000001	3	2014-06-26 12:53:18.117701	0	2014-06-26 12:53:18.115	DELETED	0
458	191	00000000-0000-0001-0000-000000000001	1	2014-06-26 12:54:32.046179	0	2014-06-26 12:54:32.043	NEW	0
459	191	00000000-0000-0001-0000-000000000001	2	2014-06-26 12:54:38.929693	0	2014-06-26 12:54:32.043	RENAMED	0
460	192	00000000-0000-0001-0000-000000000001	1	2014-06-26 12:55:06.706475	0	2014-06-26 12:55:06.704	NEW	0
461	192	00000000-0000-0001-0000-000000000001	2	2014-06-26 12:55:13.639442	0	2014-06-26 12:55:06.704	RENAMED	0
462	193	00000000-0000-0001-0000-000000000001	1	2014-06-26 12:55:27.112073	63832453	2014-06-26 12:55:27.105	NEW	4
463	194	00000000-0000-0001-0000-000000000001	1	2014-06-26 12:55:31.533222	0	2014-06-26 12:55:31.53	NEW	0
464	194	00000000-0000-0001-0000-000000000001	2	2014-06-26 12:55:39.635637	0	2014-06-26 12:55:31.53	RENAMED	0
465	195	00000000-0000-0001-0000-000000000001	1	2014-06-26 12:55:47.065723	63832453	2014-06-26 12:55:47.058	NEW	4
466	192	00000000-0000-0001-0000-000000000001	3	2014-06-26 12:56:09.535769	0	2014-06-26 12:56:09.533	DELETED	0
467	194	00000000-0000-0001-0000-000000000001	3	2014-06-26 12:56:09.894154	0	2014-06-26 12:56:09.892	DELETED	0
468	195	00000000-0000-0001-0000-000000000001	2	2014-06-26 12:56:11.258138	63832453	2014-06-26 12:56:11.256	DELETED	4
469	193	00000000-0000-0001-0000-000000000001	2	2014-06-26 12:56:11.60289	63832453	2014-06-26 12:56:11.6	DELETED	4
470	196	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:07:32.110342	63832453	2014-06-26 13:07:32.103	NEW	4
471	197	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:07:39.400649	63832453	2014-06-26 13:07:39.393	NEW	4
472	198	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:07:45.88915	63832453	2014-06-26 13:07:45.882	NEW	4
473	199	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:07:55.952337	63832453	2014-06-26 13:07:55.946	NEW	4
474	200	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:08:03.609505	63832453	2014-06-26 13:08:03.603	NEW	4
475	201	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:08:10.792504	63832453	2014-06-26 13:08:10.785	NEW	4
476	202	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:08:17.397619	63832453	2014-06-26 13:08:17.39	NEW	4
477	197	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:11:34.852942	63832453	2014-06-26 13:11:34.851	DELETED	4
478	198	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:11:35.489862	63832453	2014-06-26 13:11:35.488	DELETED	4
479	199	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:11:36.048845	63832453	2014-06-26 13:11:36.046	DELETED	4
480	200	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:11:36.566177	63832453	2014-06-26 13:11:36.564	DELETED	4
481	201	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:11:37.017182	63832453	2014-06-26 13:11:37.015	DELETED	4
482	202	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:11:37.4494	63832453	2014-06-26 13:11:37.447	DELETED	4
483	196	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:11:37.900808	63832453	2014-06-26 13:11:37.898	DELETED	4
484	203	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:12:04.200487	138936928	2014-06-26 13:12:04.193	NEW	6
485	204	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:12:13.405324	138936928	2014-06-26 13:12:13.397	NEW	6
486	205	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:12:19.412286	138936928	2014-06-26 13:12:19.404	NEW	6
487	206	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:12:25.602205	138936928	2014-06-26 13:12:25.594	NEW	6
488	207	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:12:32.717622	138936928	2014-06-26 13:12:32.709	NEW	6
489	208	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:12:39.574035	138936928	2014-06-26 13:12:39.566	NEW	6
490	209	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:12:46.805257	138936928	2014-06-26 13:12:46.797	NEW	6
491	210	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:12:53.760692	138936928	2014-06-26 13:12:53.753	NEW	6
492	211	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:13:03.099326	138936928	2014-06-26 13:13:03.091	NEW	6
493	212	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:13:10.86506	138936928	2014-06-26 13:13:10.857	NEW	6
494	204	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:13:30.509876	138936928	2014-06-26 13:13:30.507	DELETED	6
495	205	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:13:31.160069	138936928	2014-06-26 13:13:31.158	DELETED	6
496	206	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:13:31.619412	138936928	2014-06-26 13:13:31.617	DELETED	6
497	207	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:13:32.014483	138936928	2014-06-26 13:13:32.012	DELETED	6
498	208	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:13:32.375812	138936928	2014-06-26 13:13:32.373	DELETED	6
499	209	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:13:32.839299	138936928	2014-06-26 13:13:32.837	DELETED	6
500	210	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:13:33.281759	138936928	2014-06-26 13:13:33.279	DELETED	6
501	211	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:13:33.755488	138936928	2014-06-26 13:13:33.753	DELETED	6
502	212	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:13:34.356672	138936928	2014-06-26 13:13:34.354	DELETED	6
503	203	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:13:34.917528	138936928	2014-06-26 13:13:34.915	DELETED	6
504	191	00000000-0000-0001-0000-000000000001	3	2014-06-26 13:16:05.120934	0	2014-06-26 13:16:05.118	DELETED	0
505	213	00000000-0000-0001-0000-000000000001	1	2014-06-26 13:35:14.464463	113377856	2014-06-26 13:35:14.457	NEW	5
506	213	00000000-0000-0001-0000-000000000001	2	2014-06-26 13:35:20.532865	113377856	2014-06-26 13:35:20.53	DELETED	5
509	215	00000000-0000-0001-0000-000000000001	1	2014-06-26 14:57:39.239448	1517059482	2014-06-26 14:57:39.23	NEW	175196
510	215	00000000-0000-0001-0000-000000000001	2	2014-06-26 15:00:39.581063	1517059482	2014-06-26 15:00:39.579	DELETED	175196
512	214	00000000-0000-0001-0000-000000000001	3	2014-06-26 15:01:14.922284	831784409	2014-06-26 15:01:14.92	DELETED	16
514	218	00000000-0000-0001-0000-000000000001	1	2014-06-27 13:52:52.499751	19464340	2014-06-27 13:52:52.492	NEW	3
515	219	00000000-0000-0001-0000-000000000001	1	2014-06-27 13:52:59.757605	19464340	2014-06-27 13:52:59.75	NEW	3
516	220	00000000-0000-0001-0000-000000000001	1	2014-06-27 13:53:07.661862	19464340	2014-06-27 13:53:07.655	NEW	3
517	221	00000000-0000-0001-0000-000000000001	1	2014-06-27 13:53:13.812173	19464340	2014-06-27 13:53:13.804	NEW	3
518	222	00000000-0000-0001-0000-000000000001	1	2014-06-27 13:53:20.208701	19464340	2014-06-27 13:53:20.201	NEW	3
519	223	00000000-0000-0001-0000-000000000001	1	2014-06-27 13:53:26.541787	19464340	2014-06-27 13:53:26.534	NEW	3
520	224	00000000-0000-0001-0000-000000000001	1	2014-06-27 13:53:32.95716	19464340	2014-06-27 13:53:32.949	NEW	3
521	219	00000000-0000-0001-0000-000000000001	2	2014-06-27 13:54:01.11935	19464340	2014-06-27 13:54:01.117	DELETED	3
522	220	00000000-0000-0001-0000-000000000001	2	2014-06-27 13:54:01.713665	19464340	2014-06-27 13:54:01.711	DELETED	3
523	221	00000000-0000-0001-0000-000000000001	2	2014-06-27 13:54:02.238409	19464340	2014-06-27 13:54:02.236	DELETED	3
524	222	00000000-0000-0001-0000-000000000001	2	2014-06-27 13:54:02.754061	19464340	2014-06-27 13:54:02.752	DELETED	3
525	223	00000000-0000-0001-0000-000000000001	2	2014-06-27 13:54:03.262191	19464340	2014-06-27 13:54:03.26	DELETED	3
526	224	00000000-0000-0001-0000-000000000001	2	2014-06-27 13:54:03.803088	19464340	2014-06-27 13:54:03.8	DELETED	3
527	218	00000000-0000-0001-0000-000000000001	2	2014-06-27 13:54:04.327947	19464340	2014-06-27 13:54:04.325	DELETED	3
528	225	00000000-0000-0001-0000-000000000001	1	2014-06-28 03:57:01.383982	0	2014-06-28 03:57:01.382	NEW	0
529	226	00000000-0000-0001-0000-000000000001	1	2014-06-28 04:11:48.386605	1608348509	2014-06-28 04:11:48.378	NEW	2616876
530	227	00000000-0000-0001-0000-000000000001	1	2014-06-28 04:29:54.781661	1995727754	2014-06-28 04:29:54.773	NEW	80313
507	214	d1e18778-5bfc-4b4c-847c-58b208f52172	1	2014-06-26 14:57:04.212655	1	2014-06-26 14:56:10.114	NEW	0
508	214	d1e18778-5bfc-4b4c-847c-58b208f52172	2	2014-06-26 14:57:09.15566	831784409	2014-06-26 14:56:16.984	CHANGED	16
511	216	d1e18778-5bfc-4b4c-847c-58b208f52172	1	2014-06-26 15:01:14.169822	1517059482	2014-06-26 14:57:39.23	NEW	175196
513	217	d1e18778-5bfc-4b4c-847c-58b208f52172	1	2014-06-26 15:03:14.177977	831784409	2014-06-26 14:56:16.984	NEW	16
531	227	00000000-0000-0001-0000-000000000001	2	2014-06-28 04:30:32.764595	1995727754	2014-06-28 04:30:32.762	DELETED	80313
532	226	00000000-0000-0001-0000-000000000001	2	2014-06-28 04:30:36.183457	1608348509	2014-06-28 04:30:36.181	DELETED	2616876
533	228	00000000-0000-0001-0000-000000000001	1	2014-06-28 04:44:45.497458	521599328	2014-06-28 04:44:45.491	NEW	1927381
534	229	00000000-0000-0001-0000-000000000001	1	2014-06-28 04:56:01.007409	103547789	2014-06-28 04:56:01	NEW	83316
535	228	00000000-0000-0001-0000-000000000001	2	2014-06-28 04:56:23.50452	521599328	2014-06-28 04:56:23.502	DELETED	1927381
536	230	00000000-0000-0001-0000-000000000001	1	2014-06-30 11:52:58.03955	1964054474	2014-06-30 11:52:58.033	NEW	25176
537	231	00000000-0000-0001-0000-000000000001	1	2014-06-30 11:53:20.669366	0	2014-06-30 11:53:20.666	NEW	0
538	232	00000000-0000-0001-0000-000000000001	1	2014-07-01 16:45:23.615286	1964054474	2014-07-01 16:45:23.607	NEW	25176
539	233	00000000-0000-0001-0000-000000000001	1	2014-07-01 16:45:33.660246	2002898814	2014-07-01 16:45:33.651	NEW	6864
540	233	00000000-0000-0001-0000-000000000001	2	2014-07-01 16:45:49.771352	2002898814	2014-07-01 16:45:49.769	DELETED	6864
541	232	00000000-0000-0001-0000-000000000001	2	2014-07-01 16:45:52.220236	1964054474	2014-07-01 16:45:52.218	DELETED	25176
542	234	00000000-0000-0001-0000-000000000001	1	2014-07-01 16:46:07.579423	0	2014-07-01 16:46:07.577	NEW	0
543	235	0ed77390-a929-4c00-ab4c-81b7ba96ac20	1	2014-07-01 16:46:29.869141	1964054474	2014-07-01 16:45:23	NEW	25176
544	235	00000000-0000-0001-0000-000000000001	2	2014-07-04 14:37:34.639484	1964054474	2014-07-04 14:37:34.637	DELETED	25176
545	225	00000000-0000-0001-0000-000000000001	2	2014-07-04 14:49:51.038628	0	2014-07-04 14:49:51.036	DELETED	0
546	229	00000000-0000-0001-0000-000000000001	2	2014-07-04 14:49:51.0573	103547789	2014-07-04 14:49:51.036	DELETED	83316
547	230	00000000-0000-0001-0000-000000000001	2	2014-07-04 14:49:51.073957	1964054474	2014-07-04 14:49:51.036	DELETED	25176
548	231	00000000-0000-0001-0000-000000000001	2	2014-07-04 14:49:51.09065	0	2014-07-04 14:49:51.036	DELETED	0
549	236	00000000-0000-0001-0000-000000000001	1	2014-07-04 15:13:59.985293	0	2014-07-04 15:13:59.983	NEW	0
550	236	00000000-0000-0001-0000-000000000001	2	2014-07-04 15:14:07.689565	0	2014-07-04 15:13:59.983	RENAMED	0
551	170	00000000-0000-0001-0000-000000000001	5	2014-07-04 15:15:15.172964	2234059191	2014-06-20 17:11:15	RENAMED	26
552	179	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:16:51.507913	379192344	2014-06-26 11:52:58.251	CHANGED	10
553	179	00000000-0000-0001-0000-000000000001	3	2014-07-07 11:20:38.44463	623379771	2014-06-26 11:52:58.251	CHANGED	13
554	237	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:41:00.836042	75629005	2014-07-07 11:41:00.826	NEW	4
555	237	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:41:49.561868	204407536	2014-07-07 11:41:00.826	CHANGED	7
556	237	00000000-0000-0001-0000-000000000001	3	2014-07-07 11:46:45.387085	204407536	2014-07-07 11:46:45.385	DELETED	7
557	238	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:52:30.490366	0	2014-07-07 11:52:30.488	NEW	0
558	239	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:52:31.438661	0	2014-07-07 11:52:31.436	NEW	0
559	240	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:52:33.583692	574913855	2014-07-07 11:52:33.577	NEW	71883
560	241	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:52:36.037821	252178708	2014-07-07 11:52:36.032	NEW	94338
561	242	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:52:38.416028	574913855	2014-07-07 11:52:38.409	NEW	71883
562	238	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:52:45.549467	0	2014-07-07 11:52:45.547	DELETED	0
563	239	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:52:45.56164	0	2014-07-07 11:52:45.547	DELETED	0
564	240	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:52:45.575375	574913855	2014-07-07 11:52:45.547	DELETED	71883
565	241	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:52:45.586267	252178708	2014-07-07 11:52:45.547	DELETED	94338
566	242	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:52:45.600182	574913855	2014-07-07 11:52:45.547	DELETED	71883
567	243	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:52:53.591118	0	2014-07-07 11:52:53.589	NEW	0
568	244	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:52:54.488429	0	2014-07-07 11:52:54.486	NEW	0
569	243	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:53:11.285945	0	2014-07-07 11:53:11.284	DELETED	0
570	244	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:53:11.298961	0	2014-07-07 11:53:11.284	DELETED	0
571	245	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:53:18.010436	0	2014-07-07 11:53:18.009	NEW	0
572	246	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:53:18.945997	0	2014-07-07 11:53:18.943	NEW	0
573	245	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:53:39.399823	0	2014-07-07 11:53:39.398	DELETED	0
574	246	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:53:39.411868	0	2014-07-07 11:53:39.398	DELETED	0
575	247	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:53:59.698595	0	2014-07-07 11:53:59.697	NEW	0
576	248	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:54:00.675805	0	2014-07-07 11:54:00.673	NEW	0
577	247	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:58:49.598853	0	2014-07-07 11:58:49.597	DELETED	0
578	248	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:58:49.612015	0	2014-07-07 11:58:49.597	DELETED	0
579	249	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:58:59.461151	0	2014-07-07 11:58:59.459	NEW	0
580	250	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:59:00.51611	0	2014-07-07 11:59:00.513	NEW	0
581	251	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:59:02.61781	574913855	2014-07-07 11:59:02.611	NEW	71883
582	252	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:59:04.983988	252178708	2014-07-07 11:59:04.977	NEW	94338
583	253	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:59:07.213594	574913855	2014-07-07 11:59:07.208	NEW	71883
584	249	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:59:19.295511	0	2014-07-07 11:59:19.293	DELETED	0
585	250	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:59:19.306979	0	2014-07-07 11:59:19.293	DELETED	0
586	251	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:59:19.315342	574913855	2014-07-07 11:59:19.293	DELETED	71883
587	252	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:59:19.323614	252178708	2014-07-07 11:59:19.293	DELETED	94338
588	253	00000000-0000-0001-0000-000000000001	2	2014-07-07 11:59:19.33192	574913855	2014-07-07 11:59:19.293	DELETED	71883
589	254	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:59:27.013049	0	2014-07-07 11:59:27.011	NEW	0
590	255	00000000-0000-0001-0000-000000000001	1	2014-07-07 11:59:27.913106	0	2014-07-07 11:59:27.91	NEW	0
591	256	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:10:03.26195	0	2014-07-07 12:10:03.259	NEW	0
592	257	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:10:04.719244	0	2014-07-07 12:10:04.717	NEW	0
593	254	00000000-0000-0001-0000-000000000001	2	2014-07-07 12:12:32.234784	0	2014-07-07 12:12:32.233	DELETED	0
594	255	00000000-0000-0001-0000-000000000001	2	2014-07-07 12:12:32.247544	0	2014-07-07 12:12:32.233	DELETED	0
595	258	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:12:39.206884	0	2014-07-07 12:12:39.205	NEW	0
596	259	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:12:40.187064	0	2014-07-07 12:12:40.185	NEW	0
597	258	00000000-0000-0001-0000-000000000001	2	2014-07-07 12:23:45.566433	0	2014-07-07 12:23:45.564	DELETED	0
598	259	00000000-0000-0001-0000-000000000001	2	2014-07-07 12:23:45.577271	0	2014-07-07 12:23:45.564	DELETED	0
599	260	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:23:51.638477	0	2014-07-07 12:23:51.637	NEW	0
600	261	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:23:52.732534	0	2014-07-07 12:23:52.73	NEW	0
601	260	00000000-0000-0001-0000-000000000001	2	2014-07-07 12:27:36.447144	0	2014-07-07 12:27:36.445	DELETED	0
602	261	00000000-0000-0001-0000-000000000001	2	2014-07-07 12:27:36.461121	0	2014-07-07 12:27:36.445	DELETED	0
603	262	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:27:43.939623	0	2014-07-07 12:27:43.938	NEW	0
604	263	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:27:45.178975	0	2014-07-07 12:27:45.177	NEW	0
605	262	00000000-0000-0001-0000-000000000001	2	2014-07-07 12:33:03.394533	0	2014-07-07 12:33:03.392	DELETED	0
606	263	00000000-0000-0001-0000-000000000001	2	2014-07-07 12:33:03.408529	0	2014-07-07 12:33:03.392	DELETED	0
607	264	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:33:09.417693	0	2014-07-07 12:33:09.416	NEW	0
608	265	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:33:10.3571	0	2014-07-07 12:33:10.355	NEW	0
609	264	00000000-0000-0001-0000-000000000001	2	2014-07-07 12:35:34.183553	0	2014-07-07 12:35:34.182	DELETED	0
610	265	00000000-0000-0001-0000-000000000001	2	2014-07-07 12:35:34.204618	0	2014-07-07 12:35:34.182	DELETED	0
611	266	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:35:45.501502	0	2014-07-07 12:35:45.499	NEW	0
612	267	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:35:46.602791	0	2014-07-07 12:35:46.6	NEW	0
613	266	00000000-0000-0001-0000-000000000001	2	2014-07-07 12:39:26.483634	0	2014-07-07 12:39:26.481	DELETED	0
614	267	00000000-0000-0001-0000-000000000001	2	2014-07-07 12:39:26.494761	0	2014-07-07 12:39:26.481	DELETED	0
615	268	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:39:32.322537	0	2014-07-07 12:39:32.32	NEW	0
616	269	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:39:33.228603	0	2014-07-07 12:39:33.226	NEW	0
617	268	00000000-0000-0001-0000-000000000001	2	2014-07-07 12:43:30.714831	0	2014-07-07 12:43:30.713	DELETED	0
618	269	00000000-0000-0001-0000-000000000001	2	2014-07-07 12:43:30.726926	0	2014-07-07 12:43:30.713	DELETED	0
619	270	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:43:36.976922	0	2014-07-07 12:43:36.975	NEW	0
620	271	00000000-0000-0001-0000-000000000001	1	2014-07-07 12:43:38.023169	0	2014-07-07 12:43:38.021	NEW	0
621	270	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:00:14.480598	0	2014-07-07 13:00:14.479	DELETED	0
622	271	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:00:14.493136	0	2014-07-07 13:00:14.479	DELETED	0
623	272	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:00:23.788678	0	2014-07-07 13:00:23.787	NEW	0
624	273	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:00:25.061892	0	2014-07-07 13:00:25.06	NEW	0
625	274	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:00:27.845441	574913855	2014-07-07 13:00:27.839	NEW	71883
626	275	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:00:29.862234	252178708	2014-07-07 13:00:29.856	NEW	94338
627	276	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:00:31.369873	574913855	2014-07-07 13:00:31.363	NEW	71883
628	272	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:06:16.637304	0	2014-07-07 13:06:16.635	DELETED	0
629	273	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:06:16.648785	0	2014-07-07 13:06:16.635	DELETED	0
630	274	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:06:16.660958	574913855	2014-07-07 13:06:16.635	DELETED	71883
631	275	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:06:16.673842	252178708	2014-07-07 13:06:16.635	DELETED	94338
632	276	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:06:16.686025	574913855	2014-07-07 13:06:16.635	DELETED	71883
633	256	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:06:25.942926	0	2014-07-07 13:06:25.941	DELETED	0
634	257	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:06:25.956063	0	2014-07-07 13:06:25.941	DELETED	0
635	277	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:06:53.062737	0	2014-07-07 13:06:53.061	NEW	0
636	278	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:06:53.980016	0	2014-07-07 13:06:53.978	NEW	0
637	277	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:13:25.000945	0	2014-07-07 13:13:24.999	DELETED	0
638	278	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:13:25.012644	0	2014-07-07 13:13:24.999	DELETED	0
639	279	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:13:47.122118	0	2014-07-07 13:13:47.12	NEW	0
640	280	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:13:48.049999	0	2014-07-07 13:13:48.048	NEW	0
641	279	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:13:53.838646	0	2014-07-07 13:13:53.836	DELETED	0
642	280	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:13:53.850392	0	2014-07-07 13:13:53.836	DELETED	0
643	281	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:18:08.679226	0	2014-07-07 13:18:08.677	NEW	0
644	282	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:18:09.568718	0	2014-07-07 13:18:09.566	NEW	0
645	281	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:20:48.198693	0	2014-07-07 13:20:48.197	DELETED	0
646	282	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:20:48.209739	0	2014-07-07 13:20:48.197	DELETED	0
647	283	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:20:54.227829	0	2014-07-07 13:20:54.226	NEW	0
648	284	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:20:55.139993	0	2014-07-07 13:20:55.137	NEW	0
649	283	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:22:25.730629	0	2014-07-07 13:22:25.729	DELETED	0
650	284	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:22:25.743864	0	2014-07-07 13:22:25.729	DELETED	0
651	285	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:22:32.56183	0	2014-07-07 13:22:32.56	NEW	0
652	286	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:22:33.516999	0	2014-07-07 13:22:33.514	NEW	0
653	285	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:23:12.584715	0	2014-07-07 13:23:12.583	DELETED	0
654	286	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:23:12.596298	0	2014-07-07 13:23:12.583	DELETED	0
655	287	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:23:20.246005	0	2014-07-07 13:23:20.244	NEW	0
656	288	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:23:21.152496	0	2014-07-07 13:23:21.15	NEW	0
657	287	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:26:14.691642	0	2014-07-07 13:26:14.69	DELETED	0
658	288	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:26:14.703634	0	2014-07-07 13:26:14.69	DELETED	0
659	289	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:26:20.880996	0	2014-07-07 13:26:20.879	NEW	0
660	290	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:26:21.798912	0	2014-07-07 13:26:21.796	NEW	0
661	289	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:27:40.579359	0	2014-07-07 13:27:40.577	DELETED	0
662	290	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:27:40.592069	0	2014-07-07 13:27:40.577	DELETED	0
663	291	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:27:48.991203	0	2014-07-07 13:27:48.989	NEW	0
664	292	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:27:49.901841	0	2014-07-07 13:27:49.899	NEW	0
665	293	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:27:51.189483	0	2014-07-07 13:27:51.183	NEW	0
666	294	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:27:52.662006	0	2014-07-07 13:27:52.655	NEW	0
667	295	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:27:54.196585	0	2014-07-07 13:27:54.191	NEW	0
668	291	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:28:06.561287	0	2014-07-07 13:28:06.559	DELETED	0
669	292	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:28:06.583937	0	2014-07-07 13:28:06.559	DELETED	0
670	293	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:28:06.600646	0	2014-07-07 13:28:06.559	DELETED	0
671	294	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:28:06.617254	0	2014-07-07 13:28:06.559	DELETED	0
672	295	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:28:06.629904	0	2014-07-07 13:28:06.559	DELETED	0
673	296	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:28:35.099469	0	2014-07-07 13:28:35.097	NEW	0
674	297	00000000-0000-0001-0000-000000000001	1	2014-07-07 13:28:36.081535	0	2014-07-07 13:28:36.079	NEW	0
675	296	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:30:01.773778	0	2014-07-07 13:30:01.772	DELETED	0
676	297	00000000-0000-0001-0000-000000000001	2	2014-07-07 13:30:01.787295	0	2014-07-07 13:30:01.772	DELETED	0
677	298	00000000-0000-0001-0000-000000000001	1	2014-07-07 14:08:53.819828	0	2014-07-07 14:08:53.818	NEW	0
678	299	00000000-0000-0001-0000-000000000001	1	2014-07-07 14:08:54.884652	0	2014-07-07 14:08:54.882	NEW	0
679	298	00000000-0000-0001-0000-000000000001	2	2014-07-07 14:10:12.929365	0	2014-07-07 14:10:12.927	DELETED	0
680	299	00000000-0000-0001-0000-000000000001	2	2014-07-07 14:10:12.941383	0	2014-07-07 14:10:12.927	DELETED	0
681	300	00000000-0000-0001-0000-000000000001	1	2014-07-07 14:10:19.17785	0	2014-07-07 14:10:19.176	NEW	0
682	301	00000000-0000-0001-0000-000000000001	1	2014-07-07 14:10:20.09426	0	2014-07-07 14:10:20.092	NEW	0
683	300	00000000-0000-0001-0000-000000000001	2	2014-07-07 14:19:26.122457	0	2014-07-07 14:19:26.121	DELETED	0
684	301	00000000-0000-0001-0000-000000000001	2	2014-07-07 14:19:26.135815	0	2014-07-07 14:19:26.121	DELETED	0
685	302	00000000-0000-0001-0000-000000000001	1	2014-07-07 14:19:38.844027	0	2014-07-07 14:19:38.842	NEW	0
686	303	00000000-0000-0001-0000-000000000001	1	2014-07-07 14:19:39.904883	0	2014-07-07 14:19:39.902	NEW	0
687	304	00000000-0000-0001-0000-000000000001	1	2014-07-07 14:19:42.086767	574913855	2014-07-07 14:19:42.08	NEW	71883
688	305	00000000-0000-0001-0000-000000000001	1	2014-07-07 14:19:44.435904	252178708	2014-07-07 14:19:44.429	NEW	94338
689	306	00000000-0000-0001-0000-000000000001	1	2014-07-07 14:19:46.685287	574913855	2014-07-07 14:19:46.679	NEW	71883
690	302	00000000-0000-0001-0000-000000000001	2	2014-07-07 14:20:10.400222	0	2014-07-07 14:20:10.398	DELETED	0
691	303	00000000-0000-0001-0000-000000000001	2	2014-07-07 14:20:10.423829	0	2014-07-07 14:20:10.398	DELETED	0
692	304	00000000-0000-0001-0000-000000000001	2	2014-07-07 14:20:10.44881	574913855	2014-07-07 14:20:10.398	DELETED	71883
693	305	00000000-0000-0001-0000-000000000001	2	2014-07-07 14:20:10.465454	252178708	2014-07-07 14:20:10.398	DELETED	94338
694	306	00000000-0000-0001-0000-000000000001	2	2014-07-07 14:20:10.48212	574913855	2014-07-07 14:20:10.398	DELETED	71883
695	71	00000000-0000-0001-0000-000000000001	4	2014-07-07 14:20:19.973442	0	2014-06-06 09:55:32.873	RENAMED	0
696	71	00000000-0000-0001-0000-000000000001	5	2014-07-07 14:38:11.402463	0	2014-06-06 09:55:32.873	RENAMED	0
697	71	00000000-0000-0001-0000-000000000001	6	2014-07-07 14:38:42.360677	0	2014-06-06 09:55:32.873	RENAMED	0
698	71	00000000-0000-0001-0000-000000000001	7	2014-07-07 15:20:22.904157	0	2014-06-06 09:55:32.873	RENAMED	0
699	71	00000000-0000-0001-0000-000000000001	8	2014-07-07 15:23:06.928634	0	2014-06-06 09:55:32.873	RENAMED	0
700	71	00000000-0000-0001-0000-000000000001	9	2014-07-07 16:41:00.244197	0	2014-06-06 09:55:32.873	RENAMED	0
701	71	00000000-0000-0001-0000-000000000001	10	2014-07-07 16:41:32.130249	0	2014-06-06 09:55:32.873	RENAMED	0
702	71	00000000-0000-0001-0000-000000000001	11	2014-07-07 16:49:22.081526	0	2014-06-06 09:55:32.873	RENAMED	0
703	71	00000000-0000-0001-0000-000000000001	12	2014-07-07 16:50:09.001222	0	2014-06-06 09:55:32.873	RENAMED	0
704	71	00000000-0000-0001-0000-000000000001	13	2014-07-07 16:58:09.578757	0	2014-06-06 09:55:32.873	RENAMED	0
705	71	00000000-0000-0001-0000-000000000001	14	2014-07-07 16:58:41.379009	0	2014-06-06 09:55:32.873	RENAMED	0
706	307	00000000-0000-0001-0000-000000000001	1	2014-07-07 16:59:48.518098	0	2014-07-07 16:59:48.516	NEW	0
707	308	00000000-0000-0001-0000-000000000001	1	2014-07-07 16:59:49.569518	0	2014-07-07 16:59:49.567	NEW	0
708	309	00000000-0000-0001-0000-000000000001	1	2014-07-07 16:59:51.112982	574913855	2014-07-07 16:59:51.106	NEW	71883
709	310	00000000-0000-0001-0000-000000000001	1	2014-07-07 16:59:53.496424	252178708	2014-07-07 16:59:53.49	NEW	94338
710	311	00000000-0000-0001-0000-000000000001	1	2014-07-07 16:59:55.712527	574913855	2014-07-07 16:59:55.706	NEW	71883
711	71	00000000-0000-0001-0000-000000000001	15	2014-07-07 17:00:38.299568	0	2014-07-07 17:00:38.297	DELETED	0
712	76	00000000-0000-0001-0000-000000000001	5	2014-07-07 17:00:38.315228	574913855	2014-07-07 17:00:38.297	DELETED	71883
713	128	00000000-0000-0001-0000-000000000001	3	2014-07-07 17:00:38.324108	0	2014-07-07 17:00:38.297	DELETED	0
714	129	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:00:38.340253	252178708	2014-07-07 17:00:38.297	DELETED	94338
715	130	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:00:38.356946	574913855	2014-07-07 17:00:38.297	DELETED	71883
716	307	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:16:44.074575	0	2014-07-07 16:59:48.516	RENAMED	0
717	312	00000000-0000-0001-0000-000000000001	1	2014-07-07 17:20:38.814312	0	2014-07-07 17:20:38.813	NEW	0
718	313	00000000-0000-0001-0000-000000000001	1	2014-07-07 17:20:40.444406	574913855	2014-07-07 17:20:40.438	NEW	71883
719	314	00000000-0000-0001-0000-000000000001	1	2014-07-07 17:20:41.574523	0	2014-07-07 17:20:41.572	NEW	0
720	315	00000000-0000-0001-0000-000000000001	1	2014-07-07 17:20:43.118564	252178708	2014-07-07 17:20:43.112	NEW	94338
721	316	00000000-0000-0001-0000-000000000001	1	2014-07-07 17:20:44.918814	574913855	2014-07-07 17:20:44.912	NEW	71883
722	312	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:21:06.119142	0	2014-07-07 17:21:06.117	DELETED	0
723	313	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:21:06.129047	574913855	2014-07-07 17:21:06.117	DELETED	71883
724	314	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:21:06.142836	0	2014-07-07 17:21:06.117	DELETED	0
725	315	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:21:06.153979	252178708	2014-07-07 17:21:06.117	DELETED	94338
726	316	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:21:06.167876	574913855	2014-07-07 17:21:06.117	DELETED	71883
727	317	00000000-0000-0001-0000-000000000001	1	2014-07-07 17:21:36.571075	0	2014-07-07 17:21:36.569	NEW	0
728	318	00000000-0000-0001-0000-000000000001	1	2014-07-07 17:21:37.7695	574913855	2014-07-07 17:21:37.763	NEW	71883
729	319	00000000-0000-0001-0000-000000000001	1	2014-07-07 17:21:38.908514	0	2014-07-07 17:21:38.906	NEW	0
730	320	00000000-0000-0001-0000-000000000001	1	2014-07-07 17:21:40.343997	252178708	2014-07-07 17:21:40.337	NEW	94338
731	321	00000000-0000-0001-0000-000000000001	1	2014-07-07 17:21:41.868876	574913855	2014-07-07 17:21:41.862	NEW	71883
732	317	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:22:10.952086	0	2014-07-07 17:22:10.95	DELETED	0
733	318	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:22:10.96344	574913855	2014-07-07 17:22:10.95	DELETED	71883
734	319	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:22:10.97181	0	2014-07-07 17:22:10.95	DELETED	0
735	320	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:22:10.980037	252178708	2014-07-07 17:22:10.95	DELETED	94338
736	321	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:22:10.988365	574913855	2014-07-07 17:22:10.95	DELETED	71883
737	307	00000000-0000-0001-0000-000000000001	3	2014-07-07 17:22:20.734836	0	2014-07-07 16:59:48.516	RENAMED	0
738	309	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:24:07.384314	574913855	2014-07-07 17:24:07.382	DELETED	71883
739	310	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:24:08.188386	252178708	2014-07-07 17:24:08.186	DELETED	94338
740	307	00000000-0000-0001-0000-000000000001	4	2014-07-07 17:45:16.260911	0	2014-07-07 16:59:48.516	RENAMED	0
741	307	00000000-0000-0001-0000-000000000001	5	2014-07-07 17:45:28.872565	0	2014-07-07 16:59:48.516	RENAMED	0
742	307	00000000-0000-0001-0000-000000000001	6	2014-07-07 17:48:06.921721	0	2014-07-07 17:48:06.92	DELETED	0
743	308	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:48:06.934885	0	2014-07-07 17:48:06.92	DELETED	0
744	311	00000000-0000-0001-0000-000000000001	2	2014-07-07 17:48:06.951148	574913855	2014-07-07 17:48:06.92	DELETED	71883
745	322	00000000-0000-0001-0000-000000000001	1	2014-07-07 18:26:56.724802	0	2014-07-07 18:26:56.722	NEW	0
746	323	00000000-0000-0001-0000-000000000001	1	2014-07-07 18:27:10.893017	2226731310	2014-07-07 18:27:10.886	NEW	325
747	323	00000000-0000-0001-0000-000000000001	2	2014-07-07 18:27:16.188477	2226731310	2014-07-07 18:27:16.186	DELETED	325
748	324	00000000-0000-0001-0000-000000000001	1	2014-07-07 18:27:31.088667	247464773	2014-07-07 18:27:31.082	NEW	8
749	325	00000000-0000-0001-0000-000000000001	1	2014-07-07 18:27:44.109238	380175403	2014-07-07 18:27:44.103	NEW	10
750	322	00000000-0000-0001-0000-000000000001	2	2014-07-07 18:28:01.476056	0	2014-07-07 18:28:01.474	DELETED	0
751	325	00000000-0000-0001-0000-000000000001	2	2014-07-07 18:28:01.488995	380175403	2014-07-07 18:28:01.474	DELETED	10
752	324	00000000-0000-0001-0000-000000000001	2	2014-07-07 18:28:02.467299	247464773	2014-07-07 18:28:02.465	DELETED	8
753	155	00000000-0000-0001-0000-000000000001	4	2014-07-08 13:14:14.295836	2499810342	2014-06-20 10:11:11.031	CHANGED	61
754	155	00000000-0000-0001-0000-000000000001	5	2014-07-08 14:55:08.852201	715200143	2014-06-20 10:11:11.031	CHANGED	68
755	155	00000000-0000-0001-0000-000000000001	6	2014-07-10 12:03:18.815142	1130371799	2014-06-20 10:11:11.031	CHANGED	79
756	326	00000000-0000-0001-0000-000000000001	1	2014-07-14 12:59:46.383033	3345117347	2014-07-14 12:59:46.376	NEW	942008
757	327	00000000-0000-0001-0000-000000000001	1	2014-07-14 13:03:04.426886	147507866	2014-07-14 13:03:04.42	NEW	523717
758	327	00000000-0000-0001-0000-000000000001	2	2014-07-14 13:04:27.981868	147507866	2014-07-14 13:04:27.98	DELETED	523717
759	328	00000000-0000-0001-0000-000000000001	1	2014-07-14 13:05:30.813356	147507866	2014-07-14 13:05:30.807	NEW	523717
760	234	115559bf-9af7-471f-8965-432de5a864a5	2	2014-07-14 13:14:44.469107	0	2014-07-01 16:46:07.577	DELETED	0
761	236	115559bf-9af7-471f-8965-432de5a864a5	3	2014-07-14 13:15:34.44234	0	2014-07-14 13:02:56	RENAMED	0
763	168	115559bf-9af7-471f-8965-432de5a864a5	5	2014-07-14 13:15:49.495047	0	2014-06-20 14:26:49.762	DELETED	0
764	236	749653a2-1468-4614-83e2-f1aaeb85fa6b	4	2014-07-14 13:16:31.214608	0	2014-07-14 13:02:56	DELETED	0
765	330	749653a2-1468-4614-83e2-f1aaeb85fa6b	1	2014-07-14 13:16:46.189536	2021641899	2014-06-17 09:53:57	NEW	17429724
766	330	749653a2-1468-4614-83e2-f1aaeb85fa6b	2	2014-07-14 13:16:51.192557	2021641899	2014-06-17 09:53:57	DELETED	17429724
767	331	00000000-0000-0001-0000-000000000001	1	2014-07-16 17:39:53.685435	0	2014-07-16 17:39:53.679	NEW	0
768	331	00000000-0000-0001-0000-000000000001	2	2014-07-16 17:40:05.003628	0	2014-07-16 17:40:05	DELETED	0
769	332	00000000-0000-0001-0000-000000000001	1	2014-07-16 17:40:10.045158	0	2014-07-16 17:40:10.042	NEW	0
770	332	00000000-0000-0001-0000-000000000001	2	2014-07-16 17:40:46.614327	0	2014-07-16 17:40:46.611	RENAMED	0
771	333	00000000-0000-0001-0000-000000000001	1	2014-07-17 10:10:53.658696	0	2014-07-17 10:10:53.657	NEW	0
772	333	00000000-0000-0001-0000-000000000001	2	2014-07-17 10:13:21.347613	0	2014-07-17 10:13:21.344	RENAMED	0
773	179	00000000-0000-0001-0000-000000000001	4	2014-07-17 10:14:40.399856	2978417559	2014-07-17 10:14:40.396	CHANGED	30
774	179	00000000-0000-0001-0000-000000000001	5	2014-07-17 10:18:13.613331	3377466444	2014-07-17 10:18:13.609	CHANGED	32
775	179	00000000-0000-0001-0000-000000000001	6	2014-07-18 10:00:49.603462	787088806	2014-07-18 10:00:49.6	CHANGED	15
776	334	00000000-0000-0001-0000-000000000001	1	2014-07-21 09:42:14.345195	0	2014-07-21 09:42:14.342	NEW	0
777	335	00000000-0000-0001-0000-000000000001	1	2014-07-21 09:43:01.887965	1236033402	2014-07-21 09:43:01.879	NEW	207417
782	338	00000000-0000-0001-0000-000000000001	1	2014-07-22 14:22:11.626822	1368824896	2014-07-22 14:22:11.612	NEW	2187725
783	338	00000000-0000-0001-0000-000000000001	2	2014-07-22 16:31:09.936046	1368824896	2014-07-22 16:31:09.933	DELETED	2187725
784	339	00000000-0000-0001-0000-000000000001	1	2014-07-22 16:31:22.108152	1368824896	2014-07-22 16:31:22.092	NEW	2187725
785	339	00000000-0000-0001-0000-000000000001	2	2014-07-22 16:37:51.177878	1368824896	2014-07-22 16:37:51.175	DELETED	2187725
786	340	00000000-0000-0001-0000-000000000001	1	2014-07-22 16:47:47.213927	1368824896	2014-07-22 16:47:47.204	NEW	2187725
787	341	00000000-0000-0001-0000-000000000001	1	2014-07-23 10:40:26.172241	1710455513	2014-07-23 10:40:26.163	NEW	88722
788	342	00000000-0000-0001-0000-000000000001	1	2014-07-23 10:40:43.645674	1007437293	2014-07-23 10:40:43.636	NEW	669036
789	343	00000000-0000-0001-0000-000000000001	1	2014-07-23 10:41:02.588685	3145679880	2014-07-23 10:41:02.579	NEW	198658
790	344	00000000-0000-0001-0000-000000000001	1	2014-07-23 10:41:27.805189	421634651	2014-07-23 10:41:27.796	NEW	6094376
791	179	00000000-0000-0001-0000-000000000001	7	2014-07-23 15:12:56.464885	787088806	2014-07-23 15:12:56.462	RENAMED	15
792	179	00000000-0000-0001-0000-000000000001	8	2014-07-23 15:13:08.594603	787088806	2014-07-23 15:13:08.591	RENAMED	15
811	352	00000000-0000-0001-0000-000000000001	1	2014-08-12 11:11:44.462657	0	2014-08-12 11:11:44.455	NEW	0
812	353	00000000-0000-0001-0000-000000000001	1	2014-08-12 11:11:53.761781	0	2014-08-12 11:11:53.757	NEW	0
813	354	00000000-0000-0001-0000-000000000001	1	2014-08-12 11:12:37.583093	3606443132	2014-08-12 11:12:37.574	NEW	121151
814	355	00000000-0000-0001-0000-000000000001	1	2014-09-18 14:27:10.791196	0	2014-09-18 14:27:10.785	NEW	0
815	356	00000000-0000-0001-0000-000000000001	1	2014-09-18 14:27:12.764568	574913855	2014-09-18 14:27:12.756	NEW	71883
816	357	00000000-0000-0001-0000-000000000001	1	2014-09-18 14:27:14.852123	252178708	2014-09-18 14:27:14.842	NEW	94338
817	357	00000000-0000-0001-0000-000000000001	2	2014-09-18 14:53:55.389397	252178708	2014-09-18 14:53:55.386	RENAMED	94338
818	356	00000000-0000-0001-0000-000000000001	2	2014-09-18 14:53:56.072604	574913855	2014-09-18 14:53:56.069	RENAMED	71883
819	357	00000000-0000-0001-0000-000000000001	3	2014-09-18 14:56:05.130468	252178708	2014-09-18 14:56:05.127	RENAMED	94338
820	356	00000000-0000-0001-0000-000000000001	3	2014-09-18 14:56:05.763327	574913855	2014-09-18 14:56:05.761	RENAMED	71883
821	358	00000000-0000-0001-0000-000000000001	1	2014-09-18 15:07:27.491997	135856724	2014-09-18 15:07:27.484	NEW	6
822	170	f59b7f62-c077-41a0-8bb6-f7cc037d4fea	6	2014-10-16 16:00:32.069992	2234059191	2014-06-20 17:11:15	DELETED	26
823	359	00000000-0000-0001-0000-000000000001	1	2014-10-17 11:58:58.194206	0	2014-10-17 11:58:58.191	NEW	0
825	361	7f56f422-f7d8-4d36-8458-3d94e9877ae7	1	2014-10-17 12:22:27.062403	1665495150	2013-03-04 10:50:08	NEW	525662
826	362	00000000-0000-0001-0000-000000000001	1	2014-10-17 15:16:39.563631	1157805766	2014-10-17 15:16:39.553	NEW	120964
827	363	00000000-0000-0001-0000-000000000001	1	2014-10-17 15:35:01.185596	0	2014-10-17 15:35:01.182	NEW	0
828	364	00000000-0000-0001-0000-000000000001	1	2014-10-17 15:36:44.238701	3030916517	2014-10-17 15:36:44.23	NEW	2146889
829	363	00000000-0000-0001-0000-000000000001	2	2014-10-17 15:37:07.072956	0	2014-10-17 15:37:07.069	RENAMED	0
830	364	00000000-0000-0001-0000-000000000001	2	2014-10-17 15:38:15.089513	3030916517	2014-10-17 15:38:15.086	RENAMED	2146889
831	365	00000000-0000-0001-0000-000000000001	1	2014-10-17 15:39:08.016778	3030916517	2014-10-17 15:39:08.008	NEW	2146889
832	365	00000000-0000-0001-0000-000000000001	2	2014-10-17 15:39:37.696576	3030916517	2014-10-17 15:39:37.693	RENAMED	2146889
833	365	00000000-0000-0001-0000-000000000001	3	2014-10-17 15:46:07.544716	3030916517	2014-10-17 15:46:07.542	DELETED	2146889
834	364	00000000-0000-0001-0000-000000000001	3	2014-10-17 15:46:22.502661	3030916517	2014-10-17 15:46:22.499	DELETED	2146889
835	366	00000000-0000-0001-0000-000000000001	1	2014-10-17 15:46:39.777732	3030916517	2014-10-17 15:46:39.768	NEW	2146889
836	366	00000000-0000-0001-0000-000000000001	2	2014-10-17 15:47:22.7979	3030916517	2014-10-17 15:47:22.795	RENAMED	2146889
837	367	00000000-0000-0001-0000-000000000001	1	2014-10-17 16:29:11.009628	820829545	2014-10-17 16:29:11.001	NEW	46533
838	367	00000000-0000-0001-0000-000000000001	2	2014-10-17 16:29:38.933864	820829545	2014-10-17 16:29:38.93	RENAMED	46533
839	363	00000000-0000-0001-0000-000000000001	3	2014-10-17 17:06:16.083794	0	2014-10-17 17:06:16.08	DELETED	0
842	370	00000000-0000-0001-0000-000000000001	1	2014-10-20 12:01:29.357489	0	2014-10-20 12:01:29.353	NEW	0
843	371	00000000-0000-0001-0000-000000000001	1	2014-10-20 12:03:39.372204	3859616893	2014-10-20 12:03:39.362	NEW	458233
844	217	f215b520-5595-4bec-9666-ebececa3f8c0	2	2014-10-20 12:31:12.237524	2778139086	2014-10-20 12:32:33	CHANGED	31
845	217	20283f39-2b4f-49fe-83a9-e2bc64736d4b	3	2014-10-20 12:31:57.032547	1256721861	2014-10-20 12:32:53	CHANGED	45
846	180	00000000-0000-0001-0000-000000000001	2	2014-10-20 12:35:16.692497	1887357546	2014-10-20 12:35:16.688	RENAMED	1138318
847	180	00000000-0000-0001-0000-000000000001	3	2014-10-20 12:35:28.138902	1887357546	2014-10-20 12:35:28.135	DELETED	1138318
848	217	00000000-0000-0001-0000-000000000001	4	2014-10-20 12:35:37.977782	1256721861	2014-10-20 12:35:37.973	RENAMED	45
849	217	55e3eb61-1dd0-482e-966d-e5d08f487b42	5	2014-10-20 12:36:07.61755	431100350	2014-10-20 12:32:53	CHANGED	58
850	217	b74c56c8-ed25-4a60-bcc8-1e4b18c4514f	6	2014-10-20 12:40:34.357161	486086107	2014-10-20 12:41:58	CHANGED	71
852	373	00000000-0000-0001-0000-000000000001	1	2014-10-20 15:11:34.550048	0	2014-10-20 15:11:34.548	NEW	0
853	373	00000000-0000-0001-0000-000000000001	2	2014-10-20 15:13:50.801842	0	2014-10-20 15:13:50.799	DELETED	0
871	391	cda0c833-2e62-4855-9a2b-3983ab329213	1	2014-10-20 17:28:03.454767	1580273909	2014-07-11 16:07:39	NEW	875
872	392	cda0c833-2e62-4855-9a2b-3983ab329213	1	2014-10-20 17:28:03.472912	3383065211	2014-06-17 12:38:23	NEW	420
879	366	00000000-0000-0001-0000-000000000001	3	2014-10-20 17:41:53.50628	3030916517	2014-10-20 17:41:53.503	DELETED	2146889
880	392	00000000-0000-0001-0000-000000000001	2	2014-10-20 17:41:58.423306	3383065211	2014-10-20 17:41:58.42	DELETED	420
919	417	49b7671a-ad66-4ba7-8626-0208fd0275e6	1	2014-10-22 11:44:35.012273	0	2014-10-22 11:44:39.946	NEW	0
920	418	16b83cef-98d5-4527-a871-8d2e6b4016d8	1	2014-10-22 12:31:52.249085	0	2014-10-22 12:32:07.147	NEW	0
921	418	16b83cef-98d5-4527-a871-8d2e6b4016d8	2	2014-10-22 12:32:02.149284	0	2014-10-22 12:32:07.147	RENAMED	0
922	419	16b83cef-98d5-4527-a871-8d2e6b4016d8	1	2014-10-22 12:34:32.146484	1236033402	2014-07-21 09:43:01.879	NEW	207417
935	427	cac1a5c8-8cde-477c-8ea0-7b557ee0cd79	1	2014-10-23 12:47:50.251856	0	2014-10-23 12:48:16.108	NEW	0
936	427	cac1a5c8-8cde-477c-8ea0-7b557ee0cd79	2	2014-10-23 12:48:00.183093	0	2014-10-23 12:48:16.108	RENAMED	0
937	428	cac1a5c8-8cde-477c-8ea0-7b557ee0cd79	1	2014-10-23 12:48:10.193711	1236033402	2014-07-21 09:43:01.879	NEW	207417
938	429	56a61d9c-4cfd-4727-94eb-e7df564bbedc	1	2014-10-23 12:53:13.851777	3383065211	2014-06-17 12:38:23	NEW	420
939	429	56a61d9c-4cfd-4727-94eb-e7df564bbedc	2	2014-10-23 12:53:33.83562	605650194	2014-10-23 12:55:33	CHANGED	429
940	429	cac1a5c8-8cde-477c-8ea0-7b557ee0cd79	3	2014-10-23 12:53:45.152808	3383065211	2014-06-17 12:38:23	CHANGED	420
941	429	56a61d9c-4cfd-4727-94eb-e7df564bbedc	4	2014-10-23 12:54:08.818145	2702605554	2014-10-23 12:56:04	CHANGED	430
942	429	cac1a5c8-8cde-477c-8ea0-7b557ee0cd79	5	2014-10-23 12:55:45.142594	3383065211	2014-06-17 12:38:23	CHANGED	420
943	429	80a7cf24-874d-40eb-bf2d-35752a465768	6	2014-10-23 13:00:43.255277	154172221	2014-10-23 13:01:02.521	CHANGED	422
944	429	56a61d9c-4cfd-4727-94eb-e7df564bbedc	7	2014-10-23 13:00:58.767845	4165958677	2014-10-23 13:02:56	CHANGED	426
945	430	80a7cf24-874d-40eb-bf2d-35752a465768	1	2014-10-23 13:02:33.217967	0	2014-10-23 13:02:55.356	NEW	0
946	428	56a61d9c-4cfd-4727-94eb-e7df564bbedc	2	2014-10-23 13:02:48.757712	1236033402	2014-07-21 09:43:01	RENAMED	207417
947	431	15f18602-901c-4c66-92ac-e8aa8bd217a9	1	2014-10-23 13:03:50.063361	486086107	2014-10-20 12:41:58	NEW	71
948	432	15f18602-901c-4c66-92ac-e8aa8bd217a9	1	2014-10-23 13:03:50.077401	1665495150	2013-03-04 10:50:08	NEW	525662
949	433	fc234060-524f-4ed2-a002-59442ae1e9f1	1	2014-10-23 17:16:33.902488	0	2014-10-23 17:18:33	NEW	4096
950	433	fc234060-524f-4ed2-a002-59442ae1e9f1	2	2014-10-23 17:16:38.862052	0	2014-10-23 17:18:33	RENAMED	0
951	434	fc234060-524f-4ed2-a002-59442ae1e9f1	1	2014-10-23 17:17:18.883109	1	2014-10-23 17:19:22	NEW	0
952	434	fc234060-524f-4ed2-a002-59442ae1e9f1	2	2014-10-23 17:18:33.852775	1	2014-10-23 17:19:22	DELETED	0
953	435	d6b8332d-166a-4658-9bf8-e6a2a3cc4d14	1	2014-10-23 17:19:33.4884	1	2014-10-23 17:19:22	NEW	0
954	436	40536ea8-2f6c-4323-a641-252a011bfca5	1	2014-10-24 16:06:23.196625	0	2014-10-24 16:08:29	NEW	4096
955	437	d1722be3-cc52-4f40-b8dd-634573d87306	1	2014-10-24 16:13:42.440199	0	2014-10-24 16:15:49	NEW	4096
956	437	d1722be3-cc52-4f40-b8dd-634573d87306	2	2014-10-24 16:15:10.628297	0	2014-10-24 16:15:49	RENAMED	0
957	438	d1722be3-cc52-4f40-b8dd-634573d87306	1	2014-10-24 16:16:37.009655	1	2014-10-24 16:18:52	NEW	0
958	438	d1722be3-cc52-4f40-b8dd-634573d87306	2	2014-10-24 16:16:42.006821	1	2014-10-24 16:18:52	RENAMED	0
959	439	d1722be3-cc52-4f40-b8dd-634573d87306	1	2014-10-24 16:17:42.075708	0	2014-10-24 16:19:53	NEW	4096
960	439	d1722be3-cc52-4f40-b8dd-634573d87306	2	2014-10-24 16:17:42.096171	0	2014-10-24 16:19:53	RENAMED	0
961	440	d1722be3-cc52-4f40-b8dd-634573d87306	1	2014-10-24 16:17:52.0493	1	2014-10-24 16:20:05	NEW	0
962	440	d1722be3-cc52-4f40-b8dd-634573d87306	2	2014-10-24 16:17:52.06795	1	2014-10-24 16:20:05	RENAMED	0
963	441	d1722be3-cc52-4f40-b8dd-634573d87306	1	2014-10-24 16:18:07.213436	1	2014-10-24 16:20:18	NEW	0
964	441	d1722be3-cc52-4f40-b8dd-634573d87306	2	2014-10-24 16:18:07.233392	1	2014-10-24 16:20:18	RENAMED	0
965	442	d1722be3-cc52-4f40-b8dd-634573d87306	1	2014-10-24 16:18:17.004711	1	2014-10-24 16:20:31	NEW	0
966	442	d1722be3-cc52-4f40-b8dd-634573d87306	2	2014-10-24 16:18:21.989911	1	2014-10-24 16:20:31	RENAMED	0
967	443	d1722be3-cc52-4f40-b8dd-634573d87306	1	2014-10-24 16:18:26.990057	1	2014-10-24 16:20:42	NEW	0
968	443	d1722be3-cc52-4f40-b8dd-634573d87306	2	2014-10-24 16:18:32.000997	1	2014-10-24 16:20:42	RENAMED	0
969	444	e4c2ddcc-790d-4240-b481-d660a27bd021	1	2014-10-27 10:52:18.536688	837813682	2014-10-27 10:55:03.129	NEW	16
970	444	5e052b78-2d68-4395-90d5-e804abfbd05c	2	2014-10-27 10:52:32.293684	126815341	2014-10-27 10:55:20	CHANGED	39
971	445	e4c2ddcc-790d-4240-b481-d660a27bd021	1	2014-10-27 10:53:41.600569	0	2014-10-27 10:56:25.737	NEW	0
972	445	e4c2ddcc-790d-4240-b481-d660a27bd021	2	2014-10-27 10:53:46.183661	0	2014-10-27 10:56:25.737	RENAMED	0
973	446	e4c2ddcc-790d-4240-b481-d660a27bd021	1	2014-10-27 10:55:36.979606	2893555163	2013-01-18 14:32:06	NEW	12938
974	447	e4c2ddcc-790d-4240-b481-d660a27bd021	1	2014-10-27 10:56:52.751693	3606532265	2013-11-27 16:28:35.354	NEW	458601
975	438	fb089c0c-5208-4600-967c-7b274a3c814c	3	2014-10-27 12:28:44.06167	1	2014-10-24 16:18:52	DELETED	0
976	440	fb089c0c-5208-4600-967c-7b274a3c814c	3	2014-10-27 12:28:44.090475	1	2014-10-24 16:20:05	DELETED	0
977	441	fb089c0c-5208-4600-967c-7b274a3c814c	3	2014-10-27 12:28:44.105922	1	2014-10-24 16:20:18	DELETED	0
978	442	fb089c0c-5208-4600-967c-7b274a3c814c	3	2014-10-27 12:28:44.122937	1	2014-10-24 16:20:31	DELETED	0
979	443	fb089c0c-5208-4600-967c-7b274a3c814c	3	2014-10-27 12:28:44.138964	1	2014-10-24 16:20:42	DELETED	0
980	436	fb089c0c-5208-4600-967c-7b274a3c814c	2	2014-10-27 12:28:58.937085	0	2014-10-24 16:08:29	DELETED	4096
981	437	fb089c0c-5208-4600-967c-7b274a3c814c	3	2014-10-27 12:28:58.960582	0	2014-10-24 16:15:49	DELETED	0
982	439	fb089c0c-5208-4600-967c-7b274a3c814c	3	2014-10-27 12:28:58.977989	0	2014-10-24 16:19:53	DELETED	0
983	448	ee13a071-43e7-45c7-9f36-710ef347d909	1	2014-10-27 12:29:28.220598	0	2014-10-27 12:32:13.131	NEW	0
984	448	ee13a071-43e7-45c7-9f36-710ef347d909	2	2014-10-27 12:29:28.246528	0	2014-10-27 12:32:13.131	RENAMED	0
985	449	ee13a071-43e7-45c7-9f36-710ef347d909	1	2014-10-27 14:33:27.041623	2325298783	2014-07-02 10:02:07.072	NEW	91694
986	450	ee13a071-43e7-45c7-9f36-710ef347d909	1	2014-10-27 14:33:27.06723	3389064216	2014-07-02 10:02:07.833	NEW	5319
987	451	ee13a071-43e7-45c7-9f36-710ef347d909	1	2014-10-27 14:33:27.084065	1624190742	2014-07-02 10:02:08.027	NEW	79926
988	452	64aa6d37-615e-4381-8aa9-6ffd1cd47267	1	2014-10-29 12:28:58.449555	0	2014-10-29 12:31:52	NEW	4096
989	453	64aa6d37-615e-4381-8aa9-6ffd1cd47267	1	2014-10-29 12:31:08.404766	3664308926	2014-06-11 12:00:44	NEW	1070642
990	454	00000000-0000-0001-0000-000000000001	1	2014-10-29 12:32:24.653167	1948259435	2014-10-29 12:32:24.634	NEW	532341
991	452	00000000-0000-0001-0000-000000000001	2	2014-10-29 12:34:36.29617	0	2014-10-29 12:34:36.293	DELETED	4096
992	455	db034907-be61-4f4b-8a64-f3c009836310	1	2014-10-29 12:55:43.981654	0	2014-10-29 12:58:37	NEW	4096
993	455	00000000-0000-0001-0000-000000000001	2	2014-10-29 12:59:43.511293	0	2014-10-29 12:59:43.508	DELETED	4096
994	417	fdc40bd9-0c0c-4a0c-8e33-ff3b4d108764	2	2014-10-29 14:55:13.203236	0	2014-10-22 11:44:39	DELETED	0
995	456	00000000-0000-0001-0000-000000000001	1	2014-10-29 18:42:43.225948	1612535419	2014-10-29 18:42:43.219	NEW	336676
996	456	00000000-0000-0001-0000-000000000001	2	2014-10-29 18:47:03.293299	1612535419	2014-10-29 18:47:03.29	RENAMED	336676
997	457	00000000-0000-0001-0000-000000000001	1	2014-10-30 09:39:56.894685	0	2014-10-30 09:39:56.888	NEW	0
998	458	00000000-0000-0001-0000-000000000001	1	2014-10-30 09:46:46.179196	0	2014-10-30 09:46:46.176	NEW	0
999	459	00000000-0000-0001-0000-000000000001	1	2014-10-31 15:52:04.24323	3979731266	2014-10-31 15:52:04.229	NEW	12453486
1000	460	00000000-0000-0001-0000-000000000001	1	2014-11-03 09:51:49.992608	0	2014-11-03 09:51:49.99	NEW	0
1001	461	00000000-0000-0001-0000-000000000001	1	2014-11-03 10:34:31.134985	0	2014-11-03 10:34:31.128	NEW	0
1002	460	00000000-0000-0001-0000-000000000001	2	2014-11-03 10:36:26.433602	0	2014-11-03 10:36:26.428	DELETED	0
1003	461	00000000-0000-0001-0000-000000000001	2	2014-11-03 10:36:26.961562	0	2014-11-03 10:36:26.957	DELETED	0
1004	462	00000000-0000-0001-0000-000000000001	1	2014-11-03 10:52:25.654211	0	2014-11-03 10:52:25.65	NEW	0
1005	463	00000000-0000-0001-0000-000000000001	1	2014-11-03 11:08:41.473843	77104982	2014-11-03 11:08:41.462	NEW	726361
1006	464	d3b26e89-fdda-4e76-9822-866790d26e62	1	2014-11-03 11:10:23.023478	4268612444	2014-11-03 11:10:35	NEW	3095538
1007	465	d3b26e89-fdda-4e76-9822-866790d26e62	1	2014-11-03 11:10:23.045718	1369566757	2014-11-03 11:10:32	NEW	208069
1008	466	d3b26e89-fdda-4e76-9822-866790d26e62	1	2014-11-03 11:10:23.062223	2010765859	2014-11-03 11:10:36	NEW	9064219
1009	467	d3b26e89-fdda-4e76-9822-866790d26e62	1	2014-11-03 11:10:27.902938	1460739535	2014-11-03 11:10:28	NEW	1312747
1010	463	d3b26e89-fdda-4e76-9822-866790d26e62	2	2014-11-03 11:11:17.905946	77104982	2014-11-03 11:08:41	RENAMED	726361
1011	467	d3b26e89-fdda-4e76-9822-866790d26e62	2	2014-11-03 11:11:32.897853	1460739535	2014-11-03 11:10:28	RENAMED	1312747
1012	466	d3b26e89-fdda-4e76-9822-866790d26e62	2	2014-11-03 11:11:47.897308	2010765859	2014-11-03 11:10:36	RENAMED	9064219
1013	464	d3b26e89-fdda-4e76-9822-866790d26e62	2	2014-11-03 11:11:52.886591	4268612444	2014-11-03 11:10:35	RENAMED	3095538
1014	465	d3b26e89-fdda-4e76-9822-866790d26e62	2	2014-11-03 11:12:02.877097	1369566757	2014-11-03 11:10:32	RENAMED	208069
1015	431	c182f272-83ae-473b-852d-acf5f1b65c6c	2	2014-11-03 15:18:51.0102	486086107	2014-10-20 12:41:58	DELETED	71
1016	432	c182f272-83ae-473b-852d-acf5f1b65c6c	2	2014-11-03 15:18:51.039512	1665495150	2013-03-04 10:50:08	DELETED	525662
1017	468	00000000-0000-0001-0000-000000000001	1	2014-11-03 15:19:25.979294	0	2014-11-03 15:19:25.976	NEW	0
1018	469	00000000-0000-0001-0000-000000000001	1	2014-11-03 15:19:54.623261	1157805766	2014-11-03 15:19:54.614	NEW	120964
1019	469	c182f272-83ae-473b-852d-acf5f1b65c6c	2	2014-11-03 15:20:05.956306	1157805766	2014-11-03 15:19:54	DELETED	120964
1020	457	00000000-0000-0001-0000-000000000001	2	2014-11-05 15:56:20.443629	0	2014-11-05 15:56:20.44	DELETED	0
1021	470	00000000-0000-0001-0000-000000000001	1	2014-11-05 15:57:05.559546	606700754	2014-11-05 15:57:05.544	NEW	1183475
1022	471	00000000-0000-0001-0000-000000000001	1	2014-11-05 15:59:14.6648	0	2014-11-05 15:59:14.661	NEW	0
1023	472	915a7782-71b3-44ba-ac47-222f2965d7bc	1	2014-11-05 16:01:50.448422	0	2013-12-09 10:06:40	NEW	4096
1024	473	915a7782-71b3-44ba-ac47-222f2965d7bc	1	2014-11-05 16:01:50.473151	1595847508	2013-07-10 17:41:34	NEW	499147
1025	474	915a7782-71b3-44ba-ac47-222f2965d7bc	1	2014-11-05 16:01:55.514424	3021324517	2013-07-10 17:42:14	NEW	869606
1026	475	915a7782-71b3-44ba-ac47-222f2965d7bc	1	2014-11-05 16:01:55.53482	4091858297	2013-07-10 17:41:43	NEW	332633
1027	476	915a7782-71b3-44ba-ac47-222f2965d7bc	1	2014-11-05 16:01:55.5513	663039848	2013-07-10 17:41:20	NEW	516264
1028	477	915a7782-71b3-44ba-ac47-222f2965d7bc	1	2014-11-05 16:01:55.56735	72337213	2013-07-10 17:42:00	NEW	1044542
1029	478	915a7782-71b3-44ba-ac47-222f2965d7bc	1	2014-11-05 16:02:15.435345	606700754	2014-11-04 11:00:39	NEW	1183475
1030	479	915a7782-71b3-44ba-ac47-222f2965d7bc	1	2014-11-05 16:02:15.45517	3934318384	2014-09-18 11:57:39	NEW	472453
1031	480	00000000-0000-0001-0000-000000000001	1	2014-11-05 16:03:06.715403	4271649705	2014-11-05 16:03:06.704	NEW	4419
1032	478	41a5cfdf-9d49-4c1c-b55f-1d47b729eb65	2	2014-11-05 17:22:10.626978	606700754	2014-11-04 11:00:39	RENAMED	1183475
1033	481	00000000-0000-0001-0000-000000000001	1	2014-11-05 17:56:57.870775	1764236451	2014-11-05 17:56:57.859	NEW	582617
1034	481	41a5cfdf-9d49-4c1c-b55f-1d47b729eb65	2	2014-11-05 17:57:30.560773	1764236451	2014-11-05 17:56:57	DELETED	582617
1035	482	00000000-0000-0001-0000-000000000001	1	2014-11-05 17:57:46.864831	1764236451	2014-11-05 17:57:46.854	NEW	582617
1036	482	41a5cfdf-9d49-4c1c-b55f-1d47b729eb65	2	2014-11-05 17:58:00.628648	1764236451	2014-11-05 17:57:46	DELETED	582617
1037	483	00000000-0000-0001-0000-000000000001	1	2014-11-05 17:58:04.829697	1764236451	2014-11-05 17:58:04.818	NEW	582617
\.


--
-- Data for Name: item_version_chunk; Type: TABLE DATA; Schema: public; Owner: stacksync_user
--

COPY item_version_chunk (item_version_id, client_chunk_name, chunk_order) FROM stdin;
2	A9D28170AE4DCBCBC1C2DE1CE46B4FB5F42013C5	1
3	A9D28170AE4DCBCBC1C2DE1CE46B4FB5F42013C5	1
922	D0540EAFBB916988F94F86B39C1D5015A9B23DDB	1
10	8169B74D49A93340B74CF73046FDAF75C1F7D7A4	1
11	807FEACF26C6E90BEDD1F9E710FB61696B09C197	1
12	0195C03F244BB6A98D35CAE356F2AC4628D8BB18	1
13	3EB74FEB9F2321E6AB6C64D84A4209A04BEADEB8	1
13	A2D7AF6C8DC30E9CBCF5D82F4FEE6DFAAF005299	2
14	8DEC7D452F39EB298ED86C4C7AE1BB3777EA7566	1
14	75ADB93804F26B903FC8D96D145D0658CA1195EC	2
15	AC5F0CECEDE0CF22DEC537689D54BE02DCF584D0	1
16	BF9661DEFA3DAECACFDE5BDE0214C4A439351D4D	1
17	C455582F41F589213A7D34CCB3954C67476077DA	1
18	711383A59FDA05336FD2CCF70C8059D1523EB41A	1
19	21CA06890D8DCC78658629F4AE88D260D2123E0A	1
20	5E00BBD4DF681B456B57832AE0A44C3D07A7B27B	1
21	5E00BBD4DF681B456B57832AE0A44C3D07A7B27B	1
22	5E00BBD4DF681B456B57832AE0A44C3D07A7B27B	1
23	08920DBB21E1023FADE761D0D1DC3A71E6DF1552	1
24	13078D1A333AA5A9C30D56D67BC151149107CA88	1
25	874348B8C6919532816202B2F64C4096C1E1AE1F	1
26	C455582F41F589213A7D34CCB3954C67476077DA	1
27	C455582F41F589213A7D34CCB3954C67476077DA	1
28	BF9661DEFA3DAECACFDE5BDE0214C4A439351D4D	1
29	BF9661DEFA3DAECACFDE5BDE0214C4A439351D4D	1
30	DF51E37C269AA94D38F93E537BF6E2020B21406C	1
31	E48539120C29878A8CF57CF8F192C572E63A3EBF	1
36	251EDC0EB5A820646BDA4E103F0F007FD55321F3	1
37	251EDC0EB5A820646BDA4E103F0F007FD55321F3	1
38	251EDC0EB5A820646BDA4E103F0F007FD55321F3	1
39	251EDC0EB5A820646BDA4E103F0F007FD55321F3	1
40	251EDC0EB5A820646BDA4E103F0F007FD55321F3	1
41	5DDF790A6D9A12294C192582F327C2EE4B4612EE	1
42	CC04E1FDC086756785334B224686F6FAEA10B09E	1
43	25E7146A23CAECF96B3752DFABC254A7203410DE	1
44	327156AB287C6AA52C8670E13163FC1BF660ADD4	1
45	340D1A40982B0C0F61656357DF820453CBE20B36	1
46	1FAF7D8B4E3A6FC572033EC6C5BC38CF1AE467FA	1
69	051E6E537F678D62374A9701029FED646F3A763A	1
71	54F6F6368478AD731154BF22E9973284611C4179	1
73	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
75	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
77	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
79	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
80	1E3633C9D2260D5131566A467958C05CF97AAD1A	1
82	C455582F41F589213A7D34CCB3954C67476077DA	1
84	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
86	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
88	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
90	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
92	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
93	AEB89AB34F4294ABBEBE93A7EE1F43CEC6D69763	1
94	78F8BB4C43C7C3E4E5883E8E9B18518C89D965FF	1
99	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
103	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
104	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
106	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
107	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
110	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
111	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
112	C455582F41F589213A7D34CCB3954C67476077DA	1
113	AEB89AB34F4294ABBEBE93A7EE1F43CEC6D69763	1
115	C455582F41F589213A7D34CCB3954C67476077DA	1
117	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
118	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
119	AEB89AB34F4294ABBEBE93A7EE1F43CEC6D69763	1
123	F6949A8C7D5B90B4A698660BBFB9431503FBB995	1
125	78F8BB4C43C7C3E4E5883E8E9B18518C89D965FF	1
139	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
140	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
141	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
142	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
143	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
144	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
145	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
146	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
147	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
148	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
149	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
150	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
151	0F59070D1A40D3616EC1DC5B78659DB04334FA76	1
157	70C881D4A26984DDCE795F6F71817C9CF4480E79	1
159	C455582F41F589213A7D34CCB3954C67476077DA	1
160	DF51E37C269AA94D38F93E537BF6E2020B21406C	1
162	1B4201B49D1B31CAEB0536539F0D321B39496041	1
165	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
167	807FEACF26C6E90BEDD1F9E710FB61696B09C197	1
168	0195C03F244BB6A98D35CAE356F2AC4628D8BB18	1
169	3EB74FEB9F2321E6AB6C64D84A4209A04BEADEB8	1
169	A2D7AF6C8DC30E9CBCF5D82F4FEE6DFAAF005299	2
170	8DEC7D452F39EB298ED86C4C7AE1BB3777EA7566	1
170	75ADB93804F26B903FC8D96D145D0658CA1195EC	2
171	AC5F0CECEDE0CF22DEC537689D54BE02DCF584D0	1
172	AC5F0CECEDE0CF22DEC537689D54BE02DCF584D0	1
173	8DEC7D452F39EB298ED86C4C7AE1BB3777EA7566	1
173	75ADB93804F26B903FC8D96D145D0658CA1195EC	2
174	3EB74FEB9F2321E6AB6C64D84A4209A04BEADEB8	1
174	A2D7AF6C8DC30E9CBCF5D82F4FEE6DFAAF005299	2
175	0195C03F244BB6A98D35CAE356F2AC4628D8BB18	1
176	807FEACF26C6E90BEDD1F9E710FB61696B09C197	1
177	AC5F0CECEDE0CF22DEC537689D54BE02DCF584D0	1
178	3EB74FEB9F2321E6AB6C64D84A4209A04BEADEB8	1
178	A2D7AF6C8DC30E9CBCF5D82F4FEE6DFAAF005299	2
179	0195C03F244BB6A98D35CAE356F2AC4628D8BB18	1
180	807FEACF26C6E90BEDD1F9E710FB61696B09C197	1
181	8DEC7D452F39EB298ED86C4C7AE1BB3777EA7566	1
181	75ADB93804F26B903FC8D96D145D0658CA1195EC	2
182	AC5F0CECEDE0CF22DEC537689D54BE02DCF584D0	1
184	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
185	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
186	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
188	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
189	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
190	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
191	78F8BB4C43C7C3E4E5883E8E9B18518C89D965FF	1
192	78F8BB4C43C7C3E4E5883E8E9B18518C89D965FF	1
197	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
200	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
201	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
203	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
205	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
206	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
207	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
208	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
215	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
216	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
220	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
221	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
223	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
225	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
226	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
232	A9D28170AE4DCBCBC1C2DE1CE46B4FB5F42013C5	1
235	0F59070D1A40D3616EC1DC5B78659DB04334FA76	1
236	A9D28170AE4DCBCBC1C2DE1CE46B4FB5F42013C5	1
238	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
240	0F59070D1A40D3616EC1DC5B78659DB04334FA76	1
241	A9D28170AE4DCBCBC1C2DE1CE46B4FB5F42013C5	1
242	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
245	A9D28170AE4DCBCBC1C2DE1CE46B4FB5F42013C5	1
262	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
263	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
268	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
269	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
274	78F8BB4C43C7C3E4E5883E8E9B18518C89D965FF	1
276	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
277	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
278	4FBAC9AAA15E2172C7CA9865FA846C289F1FD06D	1
279	F2EE3335E8E21C304475FE3FE1B8284B1CC378A5	1
280	0879ED87622591F1C0EA8DEC338E3B5F20AAFA49	1
281	8C207004C139C0E015974AF1E41E452746BD896D	1
282	A78F48221EF74758598D2BDF6F1073CCFA915B0B	1
283	A78F48221EF74758598D2BDF6F1073CCFA915B0B	1
284	E9A9044AE7663A7CA56EDEEC3DBD604D923A06C4	1
285	7D9A8E48703EBBB32883C76D97EBFD5231F52933	1
286	7D9A8E48703EBBB32883C76D97EBFD5231F52933	1
287	7D9A8E48703EBBB32883C76D97EBFD5231F52933	1
288	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
289	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
291	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
292	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
295	7D9A8E48703EBBB32883C76D97EBFD5231F52933	1
297	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
298	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
299	7D9A8E48703EBBB32883C76D97EBFD5231F52933	1
301	7D9A8E48703EBBB32883C76D97EBFD5231F52933	1
307	7D9A8E48703EBBB32883C76D97EBFD5231F52933	1
310	645CA6D6BE8D6912AC1BA4EA05B6F060F3AF157F	1
311	645CA6D6BE8D6912AC1BA4EA05B6F060F3AF157F	1
312	B0F3E8EB53931B88E1900B7FD3E73AA29B644305	1
317	F2EE3335E8E21C304475FE3FE1B8284B1CC378A5	1
318	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
321	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
322	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
323	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
324	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
325	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
335	F2EE3335E8E21C304475FE3FE1B8284B1CC378A5	1
336	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
339	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
340	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
341	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
342	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
343	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
344	0879ED87622591F1C0EA8DEC338E3B5F20AAFA49	1
345	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
347	7D9A8E48703EBBB32883C76D97EBFD5231F52933	1
348	645CA6D6BE8D6912AC1BA4EA05B6F060F3AF157F	1
360	C841265FCDE7CC320AD8808228D0AD570161829B	1
362	2F60235E522E89A5934D1FFC894EFF962CE559A4	1
363	C841265FCDE7CC320AD8808228D0AD570161829B	1
364	2F60235E522E89A5934D1FFC894EFF962CE559A4	1
369	645CA6D6BE8D6912AC1BA4EA05B6F060F3AF157F	1
372	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
373	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
374	645CA6D6BE8D6912AC1BA4EA05B6F060F3AF157F	1
377	ABC47E42486D440EB7F513C2DD7F1707EE420AD7	1
379	B4A6601F6D6551E917AF26B0E70FA20079C7D402	1
380	B4A6601F6D6551E917AF26B0E70FA20079C7D402	1
381	B4A6601F6D6551E917AF26B0E70FA20079C7D402	1
382	BE2D62B8E7A0243DA9E0A3C166512F117A535FFF	1
383	7AABFBC3E9D0D54367D63283E1DFA7B6171CEFE6	1
383	919AFEEDB4E1642BE0F885A11DBB4AE66AC7DB8D	2
383	4BF30E1EC4FBF96E6522C7B5F995FC60900877BC	3
384	95A9171D99AA942FB4481581635DB0B6C0A4F916	1
384	9741F08A8F4341B9EA004223FD41135A31ABA2A4	2
390	B4A6601F6D6551E917AF26B0E70FA20079C7D402	1
391	B4A6601F6D6551E917AF26B0E70FA20079C7D402	1
392	B4A6601F6D6551E917AF26B0E70FA20079C7D402	1
396	B4A6601F6D6551E917AF26B0E70FA20079C7D402	1
398	B4A6601F6D6551E917AF26B0E70FA20079C7D402	1
399	F3BC099565C15B8DBF388322E55342E752668A83	1
399	2D0FBEFDAB9295E8FB67A8AD00B52A337014162A	2
402	D4419124436B3B80D6FD93CB95B4F85727B2CEE5	1
403	D6ACC3E2F14C093519823860AE971FF76243E396	1
408	7EF285F9BA8B5EF34F211AB9D7D865F4DF88B82C	1
409	3769FFC51C19CACCBCCA4235253C39229CD30FE5	1
409	393A37BE9A7A2F9F89EB4E9D85145AE621F66A1E	2
409	F1E54DB132CD56EBF9B2A0EA079F65F2170A7530	3
409	EC717E1E5214E386EA5AD949C6DCDF0524852CCD	4
411	FACD82E8FCF8C774ACEAB99F607D782EB9F0464C	1
416	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
417	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
418	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
419	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
425	645CA6D6BE8D6912AC1BA4EA05B6F060F3AF157F	1
438	4E9F61A0CE261DBCE1293B06C1479B2D96BF3460	1
439	8023062C7EAAFB0F4ADE6166D34576577510FAC0	1
439	A0E53BD5CFAE0A0E3964EB624E2C77D2C6DE44F6	2
439	460F55A27A852888D01865866C1CCEC891808633	3
441	8023062C7EAAFB0F4ADE6166D34576577510FAC0	1
441	A0E53BD5CFAE0A0E3964EB624E2C77D2C6DE44F6	2
441	460F55A27A852888D01865866C1CCEC891808633	3
442	448F5FFBAFAC70AFAD86E72C69CB56CE744F31A9	1
445	C455582F41F589213A7D34CCB3954C67476077DA	1
446	DF51E37C269AA94D38F93E537BF6E2020B21406C	1
449	7E240DE74FB1ED08FA08D38063F6A6A91462A815	1
450	0E03C6205EA671D7D41A0E3AABFC9D15D97E5ED3	1
453	7E240DE74FB1ED08FA08D38063F6A6A91462A815	1
454	5BA0EE6242FEF032B69672BAAAA937A025FBD745	1
462	70C881D4A26984DDCE795F6F71817C9CF4480E79	1
465	70C881D4A26984DDCE795F6F71817C9CF4480E79	1
470	70C881D4A26984DDCE795F6F71817C9CF4480E79	1
471	70C881D4A26984DDCE795F6F71817C9CF4480E79	1
472	70C881D4A26984DDCE795F6F71817C9CF4480E79	1
473	70C881D4A26984DDCE795F6F71817C9CF4480E79	1
474	70C881D4A26984DDCE795F6F71817C9CF4480E79	1
475	70C881D4A26984DDCE795F6F71817C9CF4480E79	1
476	70C881D4A26984DDCE795F6F71817C9CF4480E79	1
484	D96A61AC59F3C3DEF1A1E0596ED5A7D0EEF60D3B	1
485	D96A61AC59F3C3DEF1A1E0596ED5A7D0EEF60D3B	1
486	D96A61AC59F3C3DEF1A1E0596ED5A7D0EEF60D3B	1
487	D96A61AC59F3C3DEF1A1E0596ED5A7D0EEF60D3B	1
488	D96A61AC59F3C3DEF1A1E0596ED5A7D0EEF60D3B	1
489	D96A61AC59F3C3DEF1A1E0596ED5A7D0EEF60D3B	1
490	D96A61AC59F3C3DEF1A1E0596ED5A7D0EEF60D3B	1
491	D96A61AC59F3C3DEF1A1E0596ED5A7D0EEF60D3B	1
492	D96A61AC59F3C3DEF1A1E0596ED5A7D0EEF60D3B	1
493	D96A61AC59F3C3DEF1A1E0596ED5A7D0EEF60D3B	1
505	78F8BB4C43C7C3E4E5883E8E9B18518C89D965FF	1
508	944FF7742ED776CE1C85543F6240A7D9F788EE9C	1
509	448F5FFBAFAC70AFAD86E72C69CB56CE744F31A9	1
511	448F5FFBAFAC70AFAD86E72C69CB56CE744F31A9	1
513	944FF7742ED776CE1C85543F6240A7D9F788EE9C	1
514	6216F8A75FD5BB3D5F22B6F9958CDEDE3FC086C2	1
515	6216F8A75FD5BB3D5F22B6F9958CDEDE3FC086C2	1
516	6216F8A75FD5BB3D5F22B6F9958CDEDE3FC086C2	1
517	6216F8A75FD5BB3D5F22B6F9958CDEDE3FC086C2	1
518	6216F8A75FD5BB3D5F22B6F9958CDEDE3FC086C2	1
519	6216F8A75FD5BB3D5F22B6F9958CDEDE3FC086C2	1
520	6216F8A75FD5BB3D5F22B6F9958CDEDE3FC086C2	1
529	4D7AD35955F821B382B77CDA408C23D936F759D4	1
529	46149E6CE15B4BF544E24A3D4C130DA65C3508FC	2
529	50CC423446120D2A11FA078A5F9EDFFEEB5BE0C8	3
529	508AF79638C3226C3729BA442DF6BAACB519D75B	4
529	4482E664532D940B71C7333494227ADEB6427A21	5
530	786CC69D2094E7D8E14F7F6E2BEE4A7E44BE87F6	1
533	ACEA37B88EFC7C8B1557B36364C6AA2C869781C7	1
533	1BFE2656D25A5D0E482B21BEF098ED619F1028C6	2
533	BD9E95E01EBF11DC775AAA5A35921F9866982275	3
533	860BCB2553EA9F8C06A4E6F19982A0C8E97D2108	4
534	29D720141053CBB135422162471ECA6C24D1DC32	1
536	08CAB93745C704BF7E17ADF18C129E219671DA54	1
538	08CAB93745C704BF7E17ADF18C129E219671DA54	1
539	B0706E7E4577759561EEB6048879E4C45F228BEF	1
543	08CAB93745C704BF7E17ADF18C129E219671DA54	1
551	D6ACC3E2F14C093519823860AE971FF76243E396	1
552	57D714923F5B86A19E72341335DB7BE5FFD69950	1
553	3CADC81954D80A34E45AB697CE0B7E84C1EE6564	1
554	C455582F41F589213A7D34CCB3954C67476077DA	1
555	DCE6583B056499764CC89ACC9CC27B7206C4E509	1
559	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
560	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
561	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
581	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
582	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
583	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
625	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
626	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
627	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
687	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
688	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
689	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
708	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
709	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
710	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
718	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
720	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
721	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
728	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
730	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
731	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
746	C406CC997AEC5BBCE5E6BD220B70483BAA97BB68	1
748	81FF739882E2EC4A888150168050E13AD68CF3AA	1
749	EDFB6544091A62C421A99A3CBB512DECC4C48600	1
753	7284221EF130AF9BBE45538C6D451D3FE18E7008	1
754	A5395EE3C13D9912CFF9DE85896F8772708E2031	1
755	F727B00AC27CAE9F80F23B0505E2B096EBB165B7	1
756	2AE2B12EF9510A02040497BB3D4AF042A5678DBC	1
756	4B79EA0D05776E7B9FE706196DAECBC18659E4BB	2
757	23219F6D7B938E5FE57BB8B9C9918BE93BA86FCF	1
759	23219F6D7B938E5FE57BB8B9C9918BE93BA86FCF	1
937	D0540EAFBB916988F94F86B39C1D5015A9B23DDB	1
940	DCA7F0A0846F73A67D4837CAFD084B8D7835854D	1
941	EEFC5A812D0A7001164F94B6987372E23BF17768	1
943	90301AF41A160D2B4D8D4A2AE49841955B10E650	1
946	D0540EAFBB916988F94F86B39C1D5015A9B23DDB	1
947	9F969B7B75B8E3BF98EEB1B06AB4AFDF40C68183	1
948	F3BC099565C15B8DBF388322E55342E752668A83	1
948	2D0FBEFDAB9295E8FB67A8AD00B52A337014162A	2
974	2A496D3D55E75CB9A478E4CFB97345984970B4FF	1
1009	B768DB3115B4C49DDB164C8036E790614E57B981	1
1009	BA815160BB9FB26B3C07792A867C0ADD991B00E7	2
1009	63134F3C32C13E0FBBB9EC08AFFE10E8F2B325C8	3
1013	F76D0FC6C3D42754176C60E06E25B0A6A7C921CA	1
1013	5AAA64FAC7E4470908EEF87D0D9499E7F6D4D9C2	2
1013	6264CDDB1EF7E4DE953E4231218D36D21BE1CB54	3
1013	6C3FA34423C912EC5AC5233F42977ED2A48CFB8D	4
1013	A43666AF40D4D57E15046EFB090F036347AF46D8	5
1013	2D5EC847D2E0CC28FBE96FBC81AB4E2EF1BEB2FD	6
1025	8DEC7D452F39EB298ED86C4C7AE1BB3777EA7566	1
1025	75ADB93804F26B903FC8D96D145D0658CA1195EC	2
1026	0195C03F244BB6A98D35CAE356F2AC4628D8BB18	1
1027	807FEACF26C6E90BEDD1F9E710FB61696B09C197	1
1028	3EB74FEB9F2321E6AB6C64D84A4209A04BEADEB8	1
1028	A2D7AF6C8DC30E9CBCF5D82F4FEE6DFAAF005299	2
1036	112DDF7E56AEE5820C9386882202174F17DB9402	2
938	DCA7F0A0846F73A67D4837CAFD084B8D7835854D	1
942	DCA7F0A0846F73A67D4837CAFD084B8D7835854D	1
944	EE6364C0CFECD3AFF2FACCC6E9C5634FFE590565	1
969	B545A8F198CC3E79CEB3056949F6674CBA080C68	1
985	C2759E3BC3AA599830EDF350B663DFAEA83A2488	1
986	09AEF3A7DC778F43D9A7B466FC441D0CE4D82DDF	1
987	1CA9C5CA4BAD3DC019DE55A4551C902A2B1E6CF8	1
989	EDA4B6E83BCD57DF5FE7BD720134BCCAF821FE0C	1
989	848D60BB320370062AB9780C328B44E7A4AC0B8A	2
989	DB1A543A2165112945FE114A25FBC81265A67BD0	3
999	F6320AA02CE7332A5884B8D59DA555AA305DE0D9	1
999	C6D3EE93BD0EA5C724ACF9C501E5110763311135	2
999	F006D84C9842081306EFAD3E8412A27DA7EBE6A2	3
999	29641F525200CA9309B305CD9CC5D7E2D093CA72	4
999	5217B12E4D7E568354CF29A193FDA3C816998841	5
999	BA3F4FBC00D525798D5B4DD4A9D67190526DC51F	6
999	C5A2C016FCEB86E69668B39C07C13EB2696D5F39	7
999	07FE8EEFBB23CF639DA373A863B2CD2EF00EE59C	8
999	2C182A94BFA6124631B42D0DA8FC479A93E2B072	9
999	494F127FDEE5765C51097C7BCA698481EABBD089	10
999	0C96F730D949D4D51B745463E66D1733DC0983E9	11
999	D3396E0D34DA448AD77310A8E0A84DD59ED617EB	12
999	08B94C5A4A6B74008AE79F80BE1EE67D70F5645F	13
999	E955C8BC81DB7A40968F6B4872ABBB5A19036C8A	14
999	3F4231CC75510A726134AE806791AA3EF9A78CBE	15
999	C3C75FF5BE777F25F04DAD3DE4E1461453C38D67	16
999	04409395B9FE259BAA09AC2BBAF6B8B50E58D252	17
999	E4268700A71BE41FD3F7E70AEED43FA77D6FFEA0	18
999	AC5AC0A5521FD1BB42C50BC346B156843AF06C54	19
999	6CB0908633E8F6DA6DA4D11F0DF5AB32E8E2F6C2	20
999	692C3E3F66A8DC74A4CF3C71820F9F82AC86C985	21
999	0696641CBA0673799F6DC030B5DCDFDF0A4715F0	22
999	C3349D496E1356111B17D870FED3E8EFB9302E31	23
999	927D48A6668C42EC6DFC74CC0F17C916EE7F7620	24
1005	ABBE3CB963D6E678A069015D7E0C09760E4BC185	1
1005	83774DA1A1F7F83CD5ADC66FDCCD29389669CAE0	2
1010	ABBE3CB963D6E678A069015D7E0C09760E4BC185	1
1010	83774DA1A1F7F83CD5ADC66FDCCD29389669CAE0	2
1014	443D8ADBB5CB4F631A6CE6D097FAEE663E1333C1	1
1018	5EA1EEF2A55509107FBA817ADED0A814C5E8A2D8	1
1021	5456B455F06A6D21A577F6BB2E359E3016C59806	1
1021	953C285AB0A6828F0E13FC1CA5C06DAA285F90B5	2
1021	FB55C84947F0831B139D00F3D85C3061FE3606DE	3
1029	5456B455F06A6D21A577F6BB2E359E3016C59806	1
1029	953C285AB0A6828F0E13FC1CA5C06DAA285F90B5	2
1029	FB55C84947F0831B139D00F3D85C3061FE3606DE	3
1030	2D0A214A9201B2038E2042FB0892A338DE1BE42E	1
1031	E73157E9D50AD3AF5780E68396CE0542A66EA889	1
1032	5456B455F06A6D21A577F6BB2E359E3016C59806	1
1032	953C285AB0A6828F0E13FC1CA5C06DAA285F90B5	2
1032	FB55C84947F0831B139D00F3D85C3061FE3606DE	3
1033	0ECA6F626A5CA93E60FCFD067A205F0493152C72	1
1033	112DDF7E56AEE5820C9386882202174F17DB9402	2
1035	0ECA6F626A5CA93E60FCFD067A205F0493152C72	1
1035	112DDF7E56AEE5820C9386882202174F17DB9402	2
1037	0ECA6F626A5CA93E60FCFD067A205F0493152C72	1
1037	112DDF7E56AEE5820C9386882202174F17DB9402	2
765	76C57C35F68731FE00E4BC601A9E4C92EAC65E4C	1
765	03E4C0DCA698B32580FA7659224A9F32929A3DB4	2
765	CECAD189C51372AD200B6A86613F1AEBC82A9417	3
765	E67904701C7389A23A9B60E831068EC9CFE4F876	4
765	28AFACE0B0F484A16F31B147A3BCDC027EA18438	5
765	E731B0BF616CF7FB6CFDC8021FF5AFDC80E1457B	6
765	FD8B8E895A8CC14A7DC20FD8E962CFAA1DB7951B	7
765	901CEFC50317CF30373C87E80CF7E2B7B0D4033B	8
765	FC2D2D605A5EE8F7B3F15562D684A0933C8510E7	9
765	E0E86F5859B66F7358DC8DDB80F9B38E178B0F7B	10
765	007D539C8138FB9F438C6DB51B44885B166EA252	11
765	C4FEE81BCFD04823623952A83B2F8DC049CAFACB	12
765	774604B47228D0785EB50712E400E6BCFF5C4274	13
765	BFCB0FA0594CE89D8B045F698B8E3865AFA3D99F	14
765	476197236A7281891104919A925DE87D6B27D520	15
765	1FBE1997340B4A6F2D5FBCAEB07C85D608A986F3	16
765	07BB282AAF3364DFBA472B4527E61ABADBC2749C	17
765	CCD40C5153EA730FD8A9C69C2E4D1C3EF1A7E5AF	18
765	4935B2DCA9CF12894D1C185F693A51D634596F12	19
765	A3EFAD472A5A1998E21BF63982C97065BE4472CE	20
765	5C1211A2FF146B59047C9857EE67BEB673B423E3	21
765	A3BA96589AA43A8412673A54EC198E33E7B5A5F5	22
765	31F609DE94C8BC6468840529D6931E69EECE4D0C	23
765	42ACD7ED44419892D1E3224414DA4D72F3944171	24
765	80DB31B6A0C8A6E5698729A532998F562E43EDBA	25
765	AADA8BA09D1A15D9CC445403922BC9943DC3B2EE	26
765	4F76F6AA3E888B1A49E72019013B02649EF1416B	27
765	BDBA285CBD54316E1304309323209936B250D4EC	28
765	F1BC944E6F2B77C8E15D2DC33027947AA996FF23	29
765	FA3E64D47CF2FF4EEB655C43B16D03362A7F67DA	30
765	E9E7138F40CF4D34E2377005D243374EEB64EA9C	31
765	F3E185E6BFB1071ED6214598127D3A3C26FC63E1	32
765	16A512160B055792A45196A3468D9F3542E8D817	33
765	BFED99452E58C27851DD735E5B7039E04361BE8D	34
766	76C57C35F68731FE00E4BC601A9E4C92EAC65E4C	1
766	03E4C0DCA698B32580FA7659224A9F32929A3DB4	2
766	CECAD189C51372AD200B6A86613F1AEBC82A9417	3
766	E67904701C7389A23A9B60E831068EC9CFE4F876	4
766	28AFACE0B0F484A16F31B147A3BCDC027EA18438	5
766	E731B0BF616CF7FB6CFDC8021FF5AFDC80E1457B	6
766	FD8B8E895A8CC14A7DC20FD8E962CFAA1DB7951B	7
766	901CEFC50317CF30373C87E80CF7E2B7B0D4033B	8
766	FC2D2D605A5EE8F7B3F15562D684A0933C8510E7	9
766	E0E86F5859B66F7358DC8DDB80F9B38E178B0F7B	10
766	007D539C8138FB9F438C6DB51B44885B166EA252	11
766	C4FEE81BCFD04823623952A83B2F8DC049CAFACB	12
766	774604B47228D0785EB50712E400E6BCFF5C4274	13
766	BFCB0FA0594CE89D8B045F698B8E3865AFA3D99F	14
766	476197236A7281891104919A925DE87D6B27D520	15
766	1FBE1997340B4A6F2D5FBCAEB07C85D608A986F3	16
766	07BB282AAF3364DFBA472B4527E61ABADBC2749C	17
766	CCD40C5153EA730FD8A9C69C2E4D1C3EF1A7E5AF	18
766	4935B2DCA9CF12894D1C185F693A51D634596F12	19
766	A3EFAD472A5A1998E21BF63982C97065BE4472CE	20
766	5C1211A2FF146B59047C9857EE67BEB673B423E3	21
766	A3BA96589AA43A8412673A54EC198E33E7B5A5F5	22
766	31F609DE94C8BC6468840529D6931E69EECE4D0C	23
766	42ACD7ED44419892D1E3224414DA4D72F3944171	24
766	80DB31B6A0C8A6E5698729A532998F562E43EDBA	25
766	AADA8BA09D1A15D9CC445403922BC9943DC3B2EE	26
766	4F76F6AA3E888B1A49E72019013B02649EF1416B	27
766	BDBA285CBD54316E1304309323209936B250D4EC	28
766	F1BC944E6F2B77C8E15D2DC33027947AA996FF23	29
766	FA3E64D47CF2FF4EEB655C43B16D03362A7F67DA	30
766	E9E7138F40CF4D34E2377005D243374EEB64EA9C	31
766	F3E185E6BFB1071ED6214598127D3A3C26FC63E1	32
766	16A512160B055792A45196A3468D9F3542E8D817	33
766	BFED99452E58C27851DD735E5B7039E04361BE8D	34
773	E647AE3F02EC329F25589E3AB264A2171A3FDF7C	1
774	47762B45BD735814B4D805039A0AD1D91516C86E	1
775	035CCE6768FC672CF9482D70AEA98FD4A52F04DF	1
777	D0540EAFBB916988F94F86B39C1D5015A9B23DDB	1
782	5E811925375FDB2F15FFB9810F8DCE62B68C17B5	1
782	0723916D40D445B8EEE0D2530459CE3221E50793	2
782	84DFEE403026EBEB58AA3B5166329AF5CE65504F	3
782	09CCA6D44C1EDDD87A99421DC7556CBF81D3987C	4
782	AE69733EC6E5CC37682BA87CFA577BF86A474DC5	5
784	5E811925375FDB2F15FFB9810F8DCE62B68C17B5	1
784	0723916D40D445B8EEE0D2530459CE3221E50793	2
784	84DFEE403026EBEB58AA3B5166329AF5CE65504F	3
784	09CCA6D44C1EDDD87A99421DC7556CBF81D3987C	4
784	AE69733EC6E5CC37682BA87CFA577BF86A474DC5	5
786	5E811925375FDB2F15FFB9810F8DCE62B68C17B5	1
786	0723916D40D445B8EEE0D2530459CE3221E50793	2
786	84DFEE403026EBEB58AA3B5166329AF5CE65504F	3
786	09CCA6D44C1EDDD87A99421DC7556CBF81D3987C	4
786	AE69733EC6E5CC37682BA87CFA577BF86A474DC5	5
787	E92ABBF61DF8A580B2E4E89239588C8AEDF105FC	1
788	843901E8E2B88E3D4E6624E6CE365C3ED13E4FE6	1
788	FCC516C62B4463D28ADF86F267E3CAB0FA7C386A	2
789	86FBED776FB46E1534E2210078BB1387AA45A938	1
790	C22B463EB85423F67992EB7EA8952AF32FC50E0A	1
790	D842A2A03F36C35C97BC9F65A807290B86670F8C	2
790	F82ECF7D0D1AA66AD3EFA06FED35C035457C9B85	3
790	36103F16DDE6DC3DCD66FFA47EEFAB0351B0945B	4
790	2A597905E1D6ABE8A1E4CD9DEFD6E73FDA8C5B6E	5
790	8FC76A0AF831FD5326A6763D0E3404C1260D5CC5	6
790	571D260B3A05AF348368D6E54F13BE3BD1B5A728	7
790	AF9D130D6994BA553BE79D96142E555B069D5888	8
790	1548783352F31F391FF59EF5B184B3579AA04D35	9
790	2B4714D5EE8E0182E1B698E289396398F12EC6FF	10
790	1F8B6EB68D39DAEA103C633F5BB7134A0D07D0D6	11
790	1E34105C4F479850ACD38CD6975A8EEEAE9946F8	12
791	035CCE6768FC672CF9482D70AEA98FD4A52F04DF	1
792	035CCE6768FC672CF9482D70AEA98FD4A52F04DF	1
813	311F46E04BD54C6F8A1F6720857BB44B99F3EF42	1
815	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
816	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
817	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
818	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
819	FB162B501A8BEC5664022BE7C94FB283ACA15654	1
820	3717FFEEE1B2B64BE32952DA9C5D87F930E7C6CB	1
821	EB52400747037092910501F13872461840AB92C5	1
822	D6ACC3E2F14C093519823860AE971FF76243E396	1
825	F3BC099565C15B8DBF388322E55342E752668A83	1
825	2D0FBEFDAB9295E8FB67A8AD00B52A337014162A	2
826	5EA1EEF2A55509107FBA817ADED0A814C5E8A2D8	1
828	5E42F40F7A638E9CE81046D50A91EDAD4DF2F192	1
828	12C40F0258542C2FCAD09966ED9E400FC67165F9	2
828	3EB63E22C6831AD595B38B2CDBECDA958C23E061	3
828	D3B731677AD71607FE7C67749FAA9A29576BBE8F	4
828	3E9A683F52091715D274366438A879001AC52227	5
830	5E42F40F7A638E9CE81046D50A91EDAD4DF2F192	1
830	12C40F0258542C2FCAD09966ED9E400FC67165F9	2
830	3EB63E22C6831AD595B38B2CDBECDA958C23E061	3
830	D3B731677AD71607FE7C67749FAA9A29576BBE8F	4
830	3E9A683F52091715D274366438A879001AC52227	5
831	5E42F40F7A638E9CE81046D50A91EDAD4DF2F192	1
831	12C40F0258542C2FCAD09966ED9E400FC67165F9	2
831	3EB63E22C6831AD595B38B2CDBECDA958C23E061	3
831	D3B731677AD71607FE7C67749FAA9A29576BBE8F	4
831	3E9A683F52091715D274366438A879001AC52227	5
832	5E42F40F7A638E9CE81046D50A91EDAD4DF2F192	1
832	12C40F0258542C2FCAD09966ED9E400FC67165F9	2
832	3EB63E22C6831AD595B38B2CDBECDA958C23E061	3
832	D3B731677AD71607FE7C67749FAA9A29576BBE8F	4
832	3E9A683F52091715D274366438A879001AC52227	5
835	5E42F40F7A638E9CE81046D50A91EDAD4DF2F192	1
835	12C40F0258542C2FCAD09966ED9E400FC67165F9	2
835	3EB63E22C6831AD595B38B2CDBECDA958C23E061	3
835	D3B731677AD71607FE7C67749FAA9A29576BBE8F	4
835	3E9A683F52091715D274366438A879001AC52227	5
836	5E42F40F7A638E9CE81046D50A91EDAD4DF2F192	1
836	12C40F0258542C2FCAD09966ED9E400FC67165F9	2
836	3EB63E22C6831AD595B38B2CDBECDA958C23E061	3
836	D3B731677AD71607FE7C67749FAA9A29576BBE8F	4
836	3E9A683F52091715D274366438A879001AC52227	5
837	24D45F3C8530596C319108B58A9B7B23E2896242	1
838	24D45F3C8530596C319108B58A9B7B23E2896242	1
843	2DD86A08595948D29568E5DE245A0A9B59B28C7B	1
844	DBF5F340E708E288074A0FEC4897CA4EE184F2CB	1
845	8C9204772C7075C68374D786A71C901F351CF474	1
846	8023062C7EAAFB0F4ADE6166D34576577510FAC0	1
846	A0E53BD5CFAE0A0E3964EB624E2C77D2C6DE44F6	2
846	460F55A27A852888D01865866C1CCEC891808633	3
848	8C9204772C7075C68374D786A71C901F351CF474	1
849	E1A923920719D41284BB14945EA51D614942D9C3	1
850	9F969B7B75B8E3BF98EEB1B06AB4AFDF40C68183	1
939	1D62035D03709BA73F56C302A6B8C0065546682E	1
871	7A45BE3325BFDD1CE701C34A603A08B226673273	1
872	DCA7F0A0846F73A67D4837CAFD084B8D7835854D	1
970	149C576C0EAABE3B1E0CF51995DECA7AEBA8023D	1
973	935C83A1AC33B8CA2709218E130643BD836ACCF3	1
990	B4F662D62A262B2D4F64273A64A53089ACF04C82	1
990	1EB9B743680925C4C0F8BDF1F47CBBDFAED76114	2
995	A2D481D4253F87510D1FD9BE8EBE85C815888C11	1
996	A2D481D4253F87510D1FD9BE8EBE85C815888C11	1
1006	F76D0FC6C3D42754176C60E06E25B0A6A7C921CA	1
1006	5AAA64FAC7E4470908EEF87D0D9499E7F6D4D9C2	2
1006	6264CDDB1EF7E4DE953E4231218D36D21BE1CB54	3
1006	6C3FA34423C912EC5AC5233F42977ED2A48CFB8D	4
1006	A43666AF40D4D57E15046EFB090F036347AF46D8	5
1006	2D5EC847D2E0CC28FBE96FBC81AB4E2EF1BEB2FD	6
1007	443D8ADBB5CB4F631A6CE6D097FAEE663E1333C1	1
1008	B777A78F619199D3DF431F6E21CAD6B386BEB5EE	1
1008	974648A808DE661C4A95A340580304909B6D3E4C	2
1008	B099FF3646D3D667784D29ABFC0FF54AD363DD47	3
1008	342E35F3534C2DDCFAA12A6671C3DD9F7A27AE5B	4
1008	59B09BAC1155CAC900E3D0839C94BC3A1A0C365D	5
1008	85005DDDC9F82CE196649FBEA0DEA795DCAA81C4	6
1008	C56723D0AE198C2EE146DEC224949A262BCB6416	7
1008	1D50AAD1E5A25BB7FF5EC7150EDFF31B9D7885D1	8
1008	6A2978D3B9710E4C50955BB0EEA6C79A01D0E253	9
1008	C1DC625BD9BE915E64FB1C1F37EBBA396F990DC8	10
1008	1DAF8A56706E0AB0CD194495B94BEDFB240E50C7	11
1008	D71CEE8C5053FCB054012F542CB622FB7836647C	12
1008	BA1587A353BB9EB626EC28AC203D42FD5003EDD1	13
1008	567FF077980E53785931BF1012279EFDE1CB230D	14
1008	6438B0306CE3D24EF5DED118FF97C9F689C8DFFF	15
1008	0A6A408D0F71D16E35D634D0E575A644B2BADBF7	16
1008	4AF001160664F99D27548CE7DCA98E2F1B845A44	17
1008	4AD6D9A7AEC277AEF70C82A2AAF6B46F0BE0EB5F	18
1011	B768DB3115B4C49DDB164C8036E790614E57B981	1
1011	BA815160BB9FB26B3C07792A867C0ADD991B00E7	2
1011	63134F3C32C13E0FBBB9EC08AFFE10E8F2B325C8	3
1012	B777A78F619199D3DF431F6E21CAD6B386BEB5EE	1
1012	974648A808DE661C4A95A340580304909B6D3E4C	2
1012	B099FF3646D3D667784D29ABFC0FF54AD363DD47	3
1012	342E35F3534C2DDCFAA12A6671C3DD9F7A27AE5B	4
1012	59B09BAC1155CAC900E3D0839C94BC3A1A0C365D	5
1012	85005DDDC9F82CE196649FBEA0DEA795DCAA81C4	6
1012	C56723D0AE198C2EE146DEC224949A262BCB6416	7
1012	1D50AAD1E5A25BB7FF5EC7150EDFF31B9D7885D1	8
1012	6A2978D3B9710E4C50955BB0EEA6C79A01D0E253	9
1012	C1DC625BD9BE915E64FB1C1F37EBBA396F990DC8	10
1012	1DAF8A56706E0AB0CD194495B94BEDFB240E50C7	11
1012	D71CEE8C5053FCB054012F542CB622FB7836647C	12
1012	BA1587A353BB9EB626EC28AC203D42FD5003EDD1	13
1012	567FF077980E53785931BF1012279EFDE1CB230D	14
1012	6438B0306CE3D24EF5DED118FF97C9F689C8DFFF	15
1012	0A6A408D0F71D16E35D634D0E575A644B2BADBF7	16
1012	4AF001160664F99D27548CE7DCA98E2F1B845A44	17
1012	4AD6D9A7AEC277AEF70C82A2AAF6B46F0BE0EB5F	18
1015	9F969B7B75B8E3BF98EEB1B06AB4AFDF40C68183	1
1016	F3BC099565C15B8DBF388322E55342E752668A83	1
1016	2D0FBEFDAB9295E8FB67A8AD00B52A337014162A	2
1019	5EA1EEF2A55509107FBA817ADED0A814C5E8A2D8	1
1024	AC5F0CECEDE0CF22DEC537689D54BE02DCF584D0	1
1034	0ECA6F626A5CA93E60FCFD067A205F0493152C72	1
1034	112DDF7E56AEE5820C9386882202174F17DB9402	2
1036	0ECA6F626A5CA93E60FCFD067A205F0493152C72	1
\.

--
-- Name: sequencer_item; Type: SEQUENCE SET; Schema: public; Owner: stacksync_user
--

SELECT pg_catalog.setval('sequencer_item', 483, true);


--
-- Name: sequencer_item_version; Type: SEQUENCE SET; Schema: public; Owner: stacksync_user
--

SELECT pg_catalog.setval('sequencer_item_version', 1037, true);


--
-- Data for Name: user1; Type: TABLE DATA; Schema: public; Owner: stacksync_user
--

COPY user1 (id, name, swift_user, swift_account, email, quota_limit, quota_used, created_at) FROM stdin;
19d14341-e6a1-4850-8b2c-0e09869629ee	tester1	tester1	AUTH_cb0c047cdaa64fdb88575faed47306f6	tester1@test.com	9999	0	2014-05-27 16:06:38.009454
56ffff9b-a18d-4638-a9c3-a542c85e52bf	cotes	cotes	AUTH_cb0c047cdaa64fdb88575faed47306f6	cotes@test.com	0	0	2014-06-03 12:44:01.556912
dedfa1ae-eb90-4886-b33c-00b0529482f1	tester2	tester2	AUTH_cb0c047cdaa64fdb88575faed47306f6	tester2@test.com	9999	0	2014-07-21 09:30:24.085041
b08303ec-60ad-4500-85f9-4a0d08a7c0c1	nec1	nec1	AUTH_cb0c047cdaa64fdb88575faed47306f6	nec1@test.com	9999	0	2014-08-12 11:08:59.119282
7323d7b0-9d6e-488b-801d-58bd4bd687f9	tester3	tester3	AUTH_cb0c047cdaa64fdb88575faed47306f6	tester3@test.com	0	0	2014-10-23 16:22:36.830382
a6993e3a-6273-4af9-9181-c7208f65d307	tester4	tester4	AUTH_cb0c047cdaa64fdb88575faed47306f6	tester4@test.com	0	0	2014-10-23 16:23:04.582503
09c58a46-a793-40f9-83b9-ee22d07a038f	lacroix	lacroix	AUTH_cb0c047cdaa64fdb88575faed47306f6	michel.lacroix@ec.europa.eu	0	0	2014-11-03 09:46:54.717748
b6406cd2-e1e0-47b7-9249-fb9e7543d97d	foerster	foerster	AUTH_cb0c047cdaa64fdb88575faed47306f6	anna.foerster@supsi.ch	0	0	2014-11-03 09:47:30.447124
021b4ff0-12e9-4595-90bd-e2a6b1c9d008	greve	greve	AUTH_cb0c047cdaa64fdb88575faed47306f6	greve@kolabsys.com	0	0	2014-11-03 09:48:48.709627
02a0a53d-ef5e-455e-9fb6-73dd291fe686	luigi	luigi	AUTH_cb0c047cdaa64fdb88575faed47306f6	luigi@lo-iacono.net	0	0	2014-11-03 09:49:09.294487
89b68d50-17ad-4b1e-bc58-5f549682b5ef	pgarcia	pgarcia	AUTH_cb0c047cdaa64fdb88575faed47306f6	pedro.garcia@urv.cat	0	0	2014-11-03 12:45:14.501899
\.


--
-- Data for Name: workspace; Type: TABLE DATA; Schema: public; Owner: stacksync_user
--

COPY workspace (id, latest_revision, owner_id, is_shared, is_encrypted, swift_container, swift_url, created_at) FROM stdin;
a0d404a4-7240-4750-89b2-3c01601b0c1d	0	19d14341-e6a1-4850-8b2c-0e09869629ee	f	f	19d14341-e6a1-4850-8b2c-0e09869629ee-default	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-05-27 16:11:36.884109
d9c10e45-002b-4240-804a-3b4e24497e5c	0	56ffff9b-a18d-4638-a9c3-a542c85e52bf	f	f	cotes	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-06-03 12:44:01.674152
6f53e591-9019-4aa7-b8ae-fa63cb9f1035	0	dedfa1ae-eb90-4886-b33c-00b0529482f1	f	f	dedfa1ae-eb90-4886-b33c-00b0529482f1-default	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-07-21 09:32:51.978129
ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	0	19d14341-e6a1-4850-8b2c-0e09869629ee	t	f	a6d74f52-0e2a-42fa-bb1b-76daef881287	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-07-21 14:45:08.143551
c644dfd0-8c02-45e6-8b95-e8da93183ded	0	19d14341-e6a1-4850-8b2c-0e09869629ee	t	f	ca550cfb-9887-4027-a91e-aa7f483bbe53	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-07-21 14:49:11.462677
b150400f-70d4-401b-b28f-6617536d43df	0	b08303ec-60ad-4500-85f9-4a0d08a7c0c1	f	f	a9d98d89073b4086a94bdaa70e791f4e-default	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-08-12 11:09:55.69641
3f76fbe0-8c37-48b5-891a-0dd9d0e61456	0	19d14341-e6a1-4850-8b2c-0e09869629ee	t	f	2a6b4cfa-63cc-455e-aaeb-f0a326015b88	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-09-22 09:21:25.11853
1e6b3715-e787-4685-b81e-cd0ac9b4f7f0	0	56ffff9b-a18d-4638-a9c3-a542c85e52bf	t	f	84f54573-be49-4fca-b437-be43fcb9600c	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-10-17 12:21:07.712286
b944f1d0-0c5e-4c2c-a170-2d84a61cc951	0	dedfa1ae-eb90-4886-b33c-00b0529482f1	t	f	89d9c9f9-fcb7-4660-898b-eb076d376ee2	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-10-22 12:59:08.473449
4fa20d25-1dd4-44de-91eb-71fc6d784e79	0	dedfa1ae-eb90-4886-b33c-00b0529482f1	t	f	44540164-f41f-453d-b1d6-1554d6addf9a	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-10-23 12:48:43.592253
244c075f-b71f-4424-a961-4b2b9890d4de	0	7323d7b0-9d6e-488b-801d-58bd4bd687f9	f	f	tester3	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-10-23 16:22:36.940936
b4d4eb00-34d7-46c0-976f-dba69a58283f	0	a6993e3a-6273-4af9-9181-c7208f65d307	f	f	tester4	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-10-23 16:23:04.696591
c6f1148d-9e0f-426a-b48c-8aef0ebb818d	0	7323d7b0-9d6e-488b-801d-58bd4bd687f9	t	f	7220d734-7022-403a-8cd1-013a728dc8f1	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-10-23 17:17:04.766434
cda89cef-4471-40cf-93cf-559fd52ea0e8	0	a6993e3a-6273-4af9-9181-c7208f65d307	t	f	101e2e90-ffc9-48ba-83fa-ee99c23576f1	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-10-27 10:54:09.44976
21f708c5-0a3d-46ee-a695-8e360bf04554	0	a6993e3a-6273-4af9-9181-c7208f65d307	t	f	ca846ad7-5703-463e-9456-bd3370b9b805	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-10-27 14:33:58.687822
5b99e136-e6e3-42f7-8e5c-a02f41041cd7	0	09c58a46-a793-40f9-83b9-ee22d07a038f	f	f	lacroix	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-11-03 09:46:54.83062
45079d5a-0ecb-4f7a-8080-690ec7a262e4	0	b6406cd2-e1e0-47b7-9249-fb9e7543d97d	f	f	foerster	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-11-03 09:47:30.551369
c556e5a1-1197-496d-bfb5-59fce784f6ef	0	021b4ff0-12e9-4595-90bd-e2a6b1c9d008	f	f	greve	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-11-03 09:48:48.816721
4dc5dec0-1a09-49da-9927-8f8666805dcf	0	02a0a53d-ef5e-455e-9fb6-73dd291fe686	f	f	luigi	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-11-03 09:49:09.396043
85abf38c-20c1-4116-a1c1-20112d23e67b	0	09c58a46-a793-40f9-83b9-ee22d07a038f	t	f	6b4aa82d-e0ac-4c5a-92f8-34421a80941a	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-11-03 11:05:23.162499
f04645c2-090d-4a8f-944f-077cf2a49d2e	0	89b68d50-17ad-4b1e-bc58-5f549682b5ef	f	f	pgarcia	http://api.stacksync.com:8080/v1/AUTH_cb0c047cdaa64fdb88575faed47306f6	2014-11-03 12:45:14.612993
\.


--
-- Data for Name: workspace_user; Type: TABLE DATA; Schema: public; Owner: stacksync_user
--

COPY workspace_user (workspace_id, user_id, workspace_name, parent_item_id, created_at, modified_at) FROM stdin;
a0d404a4-7240-4750-89b2-3c01601b0c1d	19d14341-e6a1-4850-8b2c-0e09869629ee	default	\N	2014-05-27 16:12:58.590927	2014-05-27 16:12:58.590927
d9c10e45-002b-4240-804a-3b4e24497e5c	56ffff9b-a18d-4638-a9c3-a542c85e52bf	default	\N	2014-06-03 12:44:01.781153	2014-06-03 12:44:01.781153
6f53e591-9019-4aa7-b8ae-fa63cb9f1035	dedfa1ae-eb90-4886-b33c-00b0529482f1	default	\N	2014-07-21 09:35:07.423463	2014-07-21 09:35:07.423463
ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	19d14341-e6a1-4850-8b2c-0e09869629ee	Images	\N	2014-07-21 14:45:08.157705	2014-07-21 14:45:08.157705
ddd6fde8-c2d3-4e9b-ab32-201bbc563f7d	dedfa1ae-eb90-4886-b33c-00b0529482f1	Images	\N	2014-07-21 14:45:09.152133	2014-07-21 14:45:09.152133
c644dfd0-8c02-45e6-8b95-e8da93183ded	19d14341-e6a1-4850-8b2c-0e09869629ee	Informacion	\N	2014-07-21 14:49:11.474719	2014-07-21 14:49:11.474719
c644dfd0-8c02-45e6-8b95-e8da93183ded	dedfa1ae-eb90-4886-b33c-00b0529482f1	Informacion	\N	2014-07-21 14:49:11.861346	2014-07-21 14:49:11.861346
b150400f-70d4-401b-b28f-6617536d43df	b08303ec-60ad-4500-85f9-4a0d08a7c0c1	default	\N	2014-08-12 11:11:10.249691	2014-08-12 11:11:10.249691
3f76fbe0-8c37-48b5-891a-0dd9d0e61456	19d14341-e6a1-4850-8b2c-0e09869629ee	Otro	\N	2014-09-22 09:21:25.144068	2014-09-22 09:21:25.144068
3f76fbe0-8c37-48b5-891a-0dd9d0e61456	dedfa1ae-eb90-4886-b33c-00b0529482f1	Otro	\N	2014-09-22 09:21:25.918754	2014-09-22 09:21:25.918754
1e6b3715-e787-4685-b81e-cd0ac9b4f7f0	56ffff9b-a18d-4638-a9c3-a542c85e52bf	shared	\N	2014-10-17 12:21:07.740252	2014-10-17 12:21:07.740252
1e6b3715-e787-4685-b81e-cd0ac9b4f7f0	19d14341-e6a1-4850-8b2c-0e09869629ee	shared	\N	2014-10-17 12:21:08.096315	2014-10-17 12:21:08.096315
f04645c2-090d-4a8f-944f-077cf2a49d2e	89b68d50-17ad-4b1e-bc58-5f549682b5ef	default	\N	2014-11-03 12:45:14.723675	2014-11-03 12:45:14.723675
85abf38c-20c1-4116-a1c1-20112d23e67b	89b68d50-17ad-4b1e-bc58-5f549682b5ef	deliverables	\N	2014-11-03 12:59:21.295551	2014-11-03 12:59:21.295551
b944f1d0-0c5e-4c2c-a170-2d84a61cc951	dedfa1ae-eb90-4886-b33c-00b0529482f1	cotes	\N	2014-10-22 12:59:08.492929	2014-10-22 12:59:08.492929
b944f1d0-0c5e-4c2c-a170-2d84a61cc951	56ffff9b-a18d-4638-a9c3-a542c85e52bf	cotes	\N	2014-10-22 12:59:09.709537	2014-10-22 12:59:09.709537
4fa20d25-1dd4-44de-91eb-71fc6d784e79	dedfa1ae-eb90-4886-b33c-00b0529482f1	s3	\N	2014-10-23 12:48:43.609419	2014-10-23 12:48:43.609419
4fa20d25-1dd4-44de-91eb-71fc6d784e79	56ffff9b-a18d-4638-a9c3-a542c85e52bf	s3	\N	2014-10-23 12:48:44.165242	2014-10-23 12:48:44.165242
244c075f-b71f-4424-a961-4b2b9890d4de	7323d7b0-9d6e-488b-801d-58bd4bd687f9	default	\N	2014-10-23 16:22:37.056779	2014-10-23 16:22:37.056779
b4d4eb00-34d7-46c0-976f-dba69a58283f	a6993e3a-6273-4af9-9181-c7208f65d307	default	\N	2014-10-23 16:23:04.849752	2014-10-23 16:23:04.849752
c6f1148d-9e0f-426a-b48c-8aef0ebb818d	7323d7b0-9d6e-488b-801d-58bd4bd687f9	share1	\N	2014-10-23 17:17:04.778707	2014-10-23 17:17:04.778707
c6f1148d-9e0f-426a-b48c-8aef0ebb818d	a6993e3a-6273-4af9-9181-c7208f65d307	share1	\N	2014-10-23 17:17:05.172701	2014-10-23 17:17:05.172701
cda89cef-4471-40cf-93cf-559fd52ea0e8	a6993e3a-6273-4af9-9181-c7208f65d307	share2	\N	2014-10-27 10:54:09.46179	2014-10-27 10:54:09.46179
cda89cef-4471-40cf-93cf-559fd52ea0e8	7323d7b0-9d6e-488b-801d-58bd4bd687f9	share2	\N	2014-10-27 10:54:10.171931	2014-10-27 10:54:10.171931
cda89cef-4471-40cf-93cf-559fd52ea0e8	56ffff9b-a18d-4638-a9c3-a542c85e52bf	share2	\N	2014-10-27 11:37:29.57664	2014-10-27 11:37:29.57664
21f708c5-0a3d-46ee-a695-8e360bf04554	a6993e3a-6273-4af9-9181-c7208f65d307	share3	\N	2014-10-27 14:33:58.703446	2014-10-27 14:33:58.703446
21f708c5-0a3d-46ee-a695-8e360bf04554	7323d7b0-9d6e-488b-801d-58bd4bd687f9	share3	\N	2014-10-27 14:33:59.645191	2014-10-27 14:33:59.645191
5b99e136-e6e3-42f7-8e5c-a02f41041cd7	09c58a46-a793-40f9-83b9-ee22d07a038f	default	\N	2014-11-03 09:46:55.296807	2014-11-03 09:46:55.296807
45079d5a-0ecb-4f7a-8080-690ec7a262e4	b6406cd2-e1e0-47b7-9249-fb9e7543d97d	default	\N	2014-11-03 09:47:30.657213	2014-11-03 09:47:30.657213
c556e5a1-1197-496d-bfb5-59fce784f6ef	021b4ff0-12e9-4595-90bd-e2a6b1c9d008	default	\N	2014-11-03 09:48:48.915291	2014-11-03 09:48:48.915291
4dc5dec0-1a09-49da-9927-8f8666805dcf	02a0a53d-ef5e-455e-9fb6-73dd291fe686	default	\N	2014-11-03 09:49:09.499599	2014-11-03 09:49:09.499599
85abf38c-20c1-4116-a1c1-20112d23e67b	09c58a46-a793-40f9-83b9-ee22d07a038f	deliverables	\N	2014-11-03 11:05:23.178507	2014-11-03 11:05:23.178507
85abf38c-20c1-4116-a1c1-20112d23e67b	02a0a53d-ef5e-455e-9fb6-73dd291fe686	deliverables	\N	2014-11-03 11:05:24.221084	2014-11-03 11:05:24.221084
85abf38c-20c1-4116-a1c1-20112d23e67b	b6406cd2-e1e0-47b7-9249-fb9e7543d97d	deliverables	\N	2014-11-03 11:13:19.624248	2014-11-03 11:13:19.624248
85abf38c-20c1-4116-a1c1-20112d23e67b	021b4ff0-12e9-4595-90bd-e2a6b1c9d008	deliverables	\N	2014-11-03 11:14:11.016458	2014-11-03 11:14:11.016458
\.


--ABE TABLES
--

--
-- TOC entry 2055 (class 0 OID 22711)
-- Dependencies: 180
-- Data for Name: attribute; Type: TABLE DATA; Schema: public; Owner: stacksync_user
--

COPY attribute (id, name, latest_version, public_key_component, history_list) FROM stdin;
4659032a-1437-4bcb-a8eb-bb482e1e21a3	MarketingA	0	\\x000a3d68124d9453ece9c223d3d2279b4fe2fdbfebb566ff286e8de630c061f82a6551c03c89c5a97b6ef4323f2672cc12eec7165852f66f3d5341563aa6177ba1011790840c8e71653eeca7c9f17cfd7501a0f651bfd4498939cec9cc2a0cd2f71fe6ada34c81884e51436cf874989bbdf95271abb81b60efb88e95954f93412d72	\N
40a0f744-0e82-4a22-a588-d0a8e8ad6abb	ResearchA	0	\\x006fab1d7eccdfa96fb8aeb056333e26ca7c272fd05cc7a93490d7f488f515a39a5babf02e0c440397acfe7c9d18eb7554e76443c1064d0235f41e70cde24c1b100121a5b1da3949b04ea6bfb437050341088dae087cd5d9e8b2023642e4e1159f20482e5a28bbf865dac8e469389451567cf5b5fe2eb56a9fe6c5b409849e6b011a	\N
1d637e33-24fb-40d8-852d-cd9d9382c03d	ResearchB	0	\\x013603e9a18b9980a0bae7c69c025485ba77304f874f4cd4e9b75d19100c2b8e1930df420cac2a6b4c6cfef3ffc77634ef58ef44c4af6fbd924f4ff46b11273d20000f42dc01c1852e03613d5d93aa9cbf0d98c570bf4cae092e11118d9d0eefe353ef6b7436252d3292800d65167bd93610c2754b25551b98048aa39919414c936d	\N
535b8f3b-5ca8-46bd-9e45-4bb6d10eb212	ResearchC	0	\\x0035083d778ec579db908be640a60622d94b8998498ae5b3fd445fbd92671f84ee496a2163928bfba3bfb5f46b517ec9e85e746ccf8d5324c934446681b87a98c000a8af6638ea6dbf847d45b1d3ace63b6077d11593ef2294a36a89a1c3bfbc3dcf7b32b4bc54a2eeb647c6f8cfe7ef7faac0b8026d39bf411abfb1c508a86f5f07	\N
95020ea4-f62e-4ba3-9e0f-305c97c6ec3c	DesignA	0	\\x00408710f7699007b290ac43306e2e92b2053304bb61d29219b34df558ae2536bf9720a214c1badcabe67517a2d772fd385506854432dd80e9d92b7452af8dd3b600cb4f9c6de86337c616f78b9c446c067d47c3e88abe9edd2706bad3ee307249339b3270cef254d971128ec08a6ba5db6d4498a762fc58311b84f6d09d78ffa08b	\N
fce66c9a-4faa-4386-8485-94f3e54c5bbb	DesignB	0	\\x01548c153f39e24577eaf1b1dc4348729d4f132e0e638ee291e49d76a3de927fc2cc7b908387034d42fff42a34eca67d02c55be49f859fdbadeeec619a771d087200284048eba5aae9d8b29c286083e90329e7ab088703843a8a731ae6e76c76535b8cede7ea05e05b310c2194d2dbcbaeff1e728523fb73c593deb329ab9cd7b00c	\N
8eca033e-1cdc-4891-955c-0c4395ee7f27	DirectionA	0	\\x015f7759f742e4e3c97f8bb880de01e377a0d902cc96e0a760fc6fad5b5a4599c9881c94995360e04da4791742478ddae796240d82425cb13de2bcb65406fc5560007fed4edee3727dd6926274dc42b6bdbc9a4f9fb2540b5e2acb5d335d42b72c1bbad6cec8820213fe306ba722485f288badaef09ab7a63ef29b8788bfa3a79c50	\N
1fb86fa7-8ef8-4926-b317-8ffada2ba5d6	DevTeamA	0	\\x00442e9f18b66df00bc2340fcde7559fd2378fe908200cf7da233abbd2582a17313f441fb14eaad16a93da3115fbadfc614e287c996979124621cb05b1ff2a5d92014c73918f119bd56968eaca8fa73a4168ade4c64692c9c041550c758b809a3776a22d205b890fd0d505f56db6378095b8b443b34f517522d8f8f56b811130a96f	\N
5d9560c8-e38f-462b-b5c9-b4fdd4836962	DevTeamB	0	\\x00d5dc02f302907c6b83e665396521ebf09d6c67ee2eaf696b1c23da771117b215aa7a9ca919081cfce63a472aee7bd2fc2161631015ac0de5e1a953e871385c15012ba57d9af4d718951fd8f7d353bf554514ef831193d6f8534b7d60ae3910e352242967651656f7e4f36b90966bab7ec979012127f2c9ed9e8d0d7a885e8822c4	\N
e2970cb8-bb8e-4635-8f70-e890626f6594	DevTeamC	0	\\x00c36f72526812f529f755c24dfca23df2569b3fd48fedf00cde74354fbd71e2f6ebaf308b542d43852642cf1d8a74dd769aeb6775b2c8f131a91c6bf47a1f95f100570887b2b9ea17d4077fb53b5f608f0c60f0f9c56f9dc742b6ba37123ff83808690994f23472d8bd68ba9c37170d506ee61baa5add2d147c0586eb2da412fe49	\N
909c7e19-a8b3-4aa7-b297-fba66f92541b	DevTeamD	0	\\x009438da3f1653c03f221d0323ec15ac6a722d8dad5ce90bc0cfbc76b53fa9b8ab7fcf57efa9a6508cb48c3fd0a31a0def27859d917037339c1dc404bfeae77d5301666998b09737eb81ffeedd4a43c483dddcd0a1a513c627201ff69f4991aa0d7eac3cb5f4d7d4f4460543a345e98e3854768c14a37bd91d4496269498b65f7c1f	\N
3618dd9c-be47-4889-9800-9129a8330876	ClientBasic	0	\\x01442686e7ecad15be143af841ba3a6658c4e524b27a3ce271544885e1284791ed4ae4e5534cc07d07fc240234c853ba1abeae706b49ed5a707b491d4d798e966b00e210f3d8ceb61c23c332dee505a93067918d810d537b78bbd425ae7d82bc843a85ddaabfd5252a6d327ce2c353c4f10b5a095fc39fb50af72f8ea80ce0dea4ee	\N
c2016a0f-7d03-4c76-9196-21dce41198c4	ClientCompany	0	\\x0089eda2c76aeba3efabb8d30f66700443786c39b45a1d1d47edce295b2de95b955831122f6c24b40da47780a87e0475cf0945faf1a0d00ee7fa3e182b50a7df74001b557cc8724e765e7d3a03b93b1b9e0aedcbc106a9be2c551128425e0748b068903c84e168c2c1d694879bafe9d2b50996a0b18d2269652f80523c458d71a6db	\N
5a722725-39af-4dfa-8c84-c5ebb45196b0	ClientVIP	0	\\x015ae3f96b98406400bd23c2de95669c883f28d75222bf126fc97fba90bb13380e36697d44bf5fd84c07af5bd261ae0f4b4dddf91736dc25f087c80bc2c8cff9950053e01b10a76ac1bb9eb4ab14fa4e944ab5500ed04f4d1ba10faa9da4bd22b0e356467b7530bc830777744b2f476874b19f645923c8590bb3240cfe318f25f348	\N
a5a5f031-b083-4c60-8e30-075e78d00157	ProviderA	0	\\x01347b35f36f8b920ca476f385c8c08e581b8c5778609f840682b32e5fe75580f7a598b281068f745b3f07b05baa8ad30ccb2b574cec7bbbe570239f19d74ef52e00444d86d004673f71b1dd244f52d63680ccc9664c64db12eaa33081812b65e6062ba48fe27215219c2b031a14bfdd129172b4b3f883c4a803a772a8cd103422f7	\N
ce6609be-6fe8-4088-8abb-85453a5710e0	ProviderB	0	\\x01464fb433aec1f7b777f19a74fa519b630c44ecd614b2fee24cddbe27a22b7928b91b89528629caca09dadebc02d8351fb0a8114aa2088149d62d0b6c5b7fe96f00060d21e5fdec1f71e1d46aded1f6b47f326c568f7ae7ff2c06f42ec40c658b014b29cd7375e2f4cecae163af7cca89039c28b09bfc17bf8bb3681260d73876e5	\N
e1002b45-d113-481b-9cab-4365d9bd9486	ProviderC	0	\\x00e8e43331a0fec9f17a8f4374a5ca013b0b897946f6438f540a4878aab39fc60e6c67298414e9ab76627a74521b08707e33c59f130b18ee24c8175d6c3ca92b890028669351f2221a764e5a58762e9ead22904eca987fdca94860453613c83de30d444653eac9f1d476056d700705679a3eda18556e64000e09228a5c7dd3438fae	\N
91690506-504f-411b-94e3-8e99f6639e17	ProviderD	0	\\x0037510cdf150f924919815a39ba99c19d8e25857345b7294b997ee80857ade59c57a1ea96096ede43218acdfa22c48b58ff8c5f8c3e3ac2b09e950451354e9c82008e5ea22df9a36363c45a3357cb2917c47693c2e9e2b1d74f2f90af5511b2ba0cc07678919d7a27eb8a24d3839d7f9f090427647ac2f73b8c58cf23b769403903	\N
6eec687a-acf4-4114-926d-a23b0ca31408	SupportA	0	\\x011ba2bb4f2187216a49e947fdac7547235aa6a122eb60a1f7098af8b4322a719106068563546468097e36c6a082c5596da5f9bd35ac5293289d2860e8d3d409b50119141ab0e607f082f197bcf1178e58232d3df8e972f55c5935c45d39c11d3e223fa4ab21be445f605da90a8d4423e7b2cc2a0dddb1ab4818f005c39f4b95196c	\N
4b498852-0981-46b1-bebb-fb9df93c6178	SupportB	0	\\x00569d37bda1f48efd54e14f19d985e341cf2e55cb2f461037ff086b0395aa3bba886be6b33b646ed65f8fe160be3fa13bd90fe920bd8547000d3ad5da11460fec0106376e5f16db9e77cbad6595aeb97e92e3b6f85fe5f6d12eef556691e467e880b57b993b98b2bbf00d90c2f3b4b342fcdd75f8cf3280af499a87bf45d3c5f46f	\N
29334659-83c6-4dfb-87d1-bb1fc21b733a	ProjectA	0	\\x014232d1d696f0167951cb72d7157f912888daed43685f5edb8e5acd923d5f28fac9f3c9f77e68b3d957347e52923dc96e9737dd02f0468bd25bf93e06323aeb51009ff2258d1a522716fe662283bfbd974387dbc8b8effd060ed82733e5bb85f08bcceb9c80ca886881e101fb4c992bcb1f865c2b7920a5f004e951f79aad288ecc	\N
f4e2614f-4244-4973-aa9e-3edf3cdd6732	ProjectB	0	\\x0063120d1a90829688e265f8551efb7294414def33f483d4dcf19db63b57887195f04d352a025908387711ab9e75c792a96205a29ce157d79875b33716866ca69500d78fab8cbe9e7c04c92f9d3c0ca5fc6a846d128521eeb4054fc77a1be8213b8cd64d9744ad9314877bd15d3cab56ddea6b206db70686d5c2c1271bc5cc3037a7	\N
14f2966e-58d5-4f3c-be47-374c46c37216	ProjectC	0	\\x0055d36287efef6c06c19a7b1c42b3da1925a399e60a30a901c4938b857260570e2eea3ab285802dcd53f124a1bae69b1eaaed98fe6e3312c6de234f396a4e633b0060aefe6674201e40ee488bb49d81495e961e4a823708d547c4b086f8af5fc0fbf877e37408bc2efe6aac61fc5ce2fa7575a9fec4b1d8a01d11fe03b1e56e7f55	\N
92bc9ac2-bceb-4929-b82a-91939d0db4a7	ProjectD	0	\\x012836b0f858125f56fac002a268f8408849e364486c942d4c839dca05962e14b24b35e75bb47bae498ce01e2af29aa343745e131088cf90535e632378037edbd50035511e357ea2276f2b2fa93f603a1f040657ba266cf450e5b1c23df3bf59d3c0dccd5c9b26a42ec707a19f1ceb9f8239126e29df4698d048536bcd78ccfd6609	\N
\.

--
-- TOC entry 2057 (class 0 OID 22734)
-- Dependencies: 182
-- Data for Name: abe_component; Type: TABLE DATA; Schema: public; Owner: stacksync_user
--

COPY abe_component (id, item_id, attribute, encrypted_pk_component, version) FROM stdin;
928332be-d1e2-4628-a874-e91ebab8db0a	5	40a0f744-0e82-4a22-a588-d0a8e8ad6abb	\\x000a7b6748349ea0a255644ffe752daaebf383340a3d9390488e95dd791cf2b24911bf0d4b34b29e99ab9aae9a882f046f670b566824f71267611f4fbddc617a4500fd56315e77f104b376d4de4228ab768f3b5859bb308b361dba9f28e25ac21ba0332ff4e5ae6011494786c378afacf1facaf19f1da146facce0c12d01b6ce2962	0
63ac5649-91dd-4dc8-ba2c-2bb03764c62a	5	1d637e33-24fb-40d8-852d-cd9d9382c03d	\\x0012dabcd4870cf885b948a164651b367da06425428f2bd7c7c11bb5145d8f1d60c96af33a7ae300f506162dedbf86d67b214ce6695ff30bd7847350c7e434eef600dc721638e42c08bc4b4a63db5b4b946e42811a3ed37b1b5fe564805ece2b855fc5f02feeffa6d29bca5a06056d4a6bc4e41429224776e71ce0bc16050e806ad6	0
37148009-a007-4e22-a36b-3f4d95ba7bc1	5	4659032a-1437-4bcb-a8eb-bb482e1e21a3	\\x00e8d4c39f5f9e82d18634079377fde1d79f027efddb2258c110db5844f538b89b2452118e95141aa63360bc95bd35ee5c645ba22d04def6ab932b4da26bf5d1030164b9ded50b7343fa954f9ca3961e55be8b6c18def290b532e1b4d27aa833d3826174e810cceee2fc8168ef0d604af6f283e0de5f551cd2ed6183cd2006894d52	0
\.


--
-- TOC entry 2056 (class 0 OID 22720)
-- Dependencies: 181
-- Data for Name: access_component; Type: TABLE DATA; Schema: public; Owner: stacksync_user
--

--COPY access_component (id, user_id, attribute, sk_component, version) FROM stdin;
--9a5ca527-8846-4995-b387-860a36af724d	alice@stacksync.com	4659032a-1437-4bcb-a8eb-bb482e1e21a3	\\x012b65e5690eef334ba4eaef9013badc1fbd862249a0baa24d90ae5fdc8bd7feaa7f4402c474f10dc617cba88370a1f9df6405d343e50887806330a76dbab0cdc1002b5d0f0a931b92d4fd3b403b8e4f3fc137cfa5bb4ebb82c7ac373fc96314301efff0d95ca72f05ba2ed3e19b38e366febb4971b7c6297a206c58921c48cea392	0
--b644c192-c16c-4d2a-8f17-09cce076ed96	alice@stacksync.com	40a0f744-0e82-4a22-a588-d0a8e8ad6abb	\\x00d944c514295b5b7f075860061d5bf79fcb6e2486421034662f45602a2a7f3af637623b9a66ea41fe84e9376c719fb286cff5dfdda342587e376e52cafc47f13d00c58ddf06f61d6449ff9b69a8187a71a01e14135cb715952f8584e0caa0358a8d788d91fc134fe2bbc19887fd249a1efd1dcfe02e890d2a22065bab5a6209d47e	0
--542cfea2-7558-4008-bd95-34df49c2eb28	alice@stacksync.com	1d637e33-24fb-40d8-852d-cd9d9382c03d	\\x00d19a3eb92b1557100082ad418c88dfeb8e14318d002aaa1cba3ce9cca0717f1045de9ff85ca2250a795290860fee27516b935f255d98d89d32ccada017cf99b3013b5d1c2c5f592e44a77419edf960effc4f150de366a4ef198d1ac04c149139c87091fcfafd162fe5c4d89554811b433161665a99bfe17e5c1ee85da0befc8bf3	0
--\.

--
-- TOC entry 2054 (class 0 OID 22702)
-- Dependencies: 179
-- Data for Name: curve; Type: TABLE DATA; Schema: public; Owner: stacksync_user
--

--COPY curve (id, type, q, r, h, exp1, exp2, sign0, sign1) FROM stdin;
--b3a6bd2a-cda7-4330-a52c-bf55c8d06d89	a	19194339420707709528738092420533633911675593073335385439780420465273928233136972798941717693920556548809124257695175710621264596277088112751636711363275971	730750818665451459101842416367364881864821047297	26266599955045910414579108844722330181529897685643694849248552388786749184022230332803140476258859508526276	63	159	1	1
--\.


--
-- Name: pk_device; Type: CONSTRAINT; Schema: public; Owner: stacksync_user; Tablespace: 
--

ALTER TABLE ONLY device
    ADD CONSTRAINT pk_device PRIMARY KEY (id);


--
-- Name: pk_item; Type: CONSTRAINT; Schema: public; Owner: stacksync_user; Tablespace: 
--

ALTER TABLE ONLY item
    ADD CONSTRAINT pk_item PRIMARY KEY (id);


--
-- Name: pk_item_version; Type: CONSTRAINT; Schema: public; Owner: stacksync_user; Tablespace: 
--

ALTER TABLE ONLY item_version
    ADD CONSTRAINT pk_item_version PRIMARY KEY (id);


--
-- Name: pk_item_version_chunk; Type: CONSTRAINT; Schema: public; Owner: stacksync_user; Tablespace: 
--

ALTER TABLE ONLY item_version_chunk
    ADD CONSTRAINT pk_item_version_chunk PRIMARY KEY (item_version_id, client_chunk_name, chunk_order);


--
-- Name: pk_user; Type: CONSTRAINT; Schema: public; Owner: stacksync_user; Tablespace: 
--

ALTER TABLE ONLY user1
    ADD CONSTRAINT pk_user PRIMARY KEY (id);


--
-- Name: pk_workspace; Type: CONSTRAINT; Schema: public; Owner: stacksync_user; Tablespace: 
--

ALTER TABLE ONLY workspace
    ADD CONSTRAINT pk_workspace PRIMARY KEY (id);


--
-- Name: pk_workspace_user; Type: CONSTRAINT; Schema: public; Owner: stacksync_user; Tablespace: 
--

ALTER TABLE ONLY workspace_user
    ADD CONSTRAINT pk_workspace_user PRIMARY KEY (workspace_id, user_id);


--
-- Name: user1_swift_user_key; Type: CONSTRAINT; Schema: public; Owner: stacksync_user; Tablespace: 
--

ALTER TABLE ONLY user1
    ADD CONSTRAINT user1_swift_user_key UNIQUE (swift_user);


--
-- Name: item_parent_id; Type: INDEX; Schema: public; Owner: stacksync_user; Tablespace: 
--

CREATE INDEX item_parent_id ON item USING btree (parent_id);


--
-- Name: item_version_item_id; Type: INDEX; Schema: public; Owner: stacksync_user; Tablespace: 
--

CREATE INDEX item_version_item_id ON item_version USING btree (item_id);


--
-- Name: item_workspace_id; Type: INDEX; Schema: public; Owner: stacksync_user; Tablespace: 
--

CREATE INDEX item_workspace_id ON item USING btree (workspace_id);


--
-- Name: itemid_version_unique; Type: INDEX; Schema: public; Owner: stacksync_user; Tablespace: 
--

CREATE UNIQUE INDEX itemid_version_unique ON item_version USING btree (item_id, version);


--
-- Name: fk1_device; Type: FK CONSTRAINT; Schema: public; Owner: stacksync_user
--

ALTER TABLE ONLY device
    ADD CONSTRAINT fk1_device FOREIGN KEY (user_id) REFERENCES user1(id) ON DELETE CASCADE;


--
-- Name: fk1_item; Type: FK CONSTRAINT; Schema: public; Owner: stacksync_user
--

ALTER TABLE ONLY item
    ADD CONSTRAINT fk1_item FOREIGN KEY (workspace_id) REFERENCES workspace(id) ON DELETE CASCADE;


--
-- Name: fk1_workspace; Type: FK CONSTRAINT; Schema: public; Owner: stacksync_user
--

ALTER TABLE ONLY workspace
    ADD CONSTRAINT fk1_workspace FOREIGN KEY (owner_id) REFERENCES user1(id) ON DELETE CASCADE;


--
-- Name: fk1_workspace_user; Type: FK CONSTRAINT; Schema: public; Owner: stacksync_user
--

ALTER TABLE ONLY workspace_user
    ADD CONSTRAINT fk1_workspace_user FOREIGN KEY (user_id) REFERENCES user1(id) ON DELETE CASCADE;


--
-- Name: fk2_item; Type: FK CONSTRAINT; Schema: public; Owner: stacksync_user
--

ALTER TABLE ONLY item
    ADD CONSTRAINT fk2_item FOREIGN KEY (parent_id) REFERENCES item(id) ON DELETE CASCADE;


--
-- Name: fk2_item_version; Type: FK CONSTRAINT; Schema: public; Owner: stacksync_user
--

ALTER TABLE ONLY item_version
    ADD CONSTRAINT fk2_item_version FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE;


--
-- Name: fk2_item_version_chunk; Type: FK CONSTRAINT; Schema: public; Owner: stacksync_user
--

ALTER TABLE ONLY item_version_chunk
    ADD CONSTRAINT fk2_item_version_chunk FOREIGN KEY (item_version_id) REFERENCES item_version(id) ON DELETE CASCADE;


--
-- Name: fk2_workspace_user; Type: FK CONSTRAINT; Schema: public; Owner: stacksync_user
--

ALTER TABLE ONLY workspace_user
    ADD CONSTRAINT fk2_workspace_user FOREIGN KEY (workspace_id) REFERENCES workspace(id) ON DELETE CASCADE;


--
-- Name: fk3_item_version; Type: FK CONSTRAINT; Schema: public; Owner: stacksync_user
--

ALTER TABLE ONLY item_version
    ADD CONSTRAINT fk3_item_version FOREIGN KEY (device_id) REFERENCES device(id) ON DELETE CASCADE;


--
-- Name: fk3_workspace_user; Type: FK CONSTRAINT; Schema: public; Owner: stacksync_user
--

ALTER TABLE ONLY workspace_user
    ADD CONSTRAINT fk3_workspace_user FOREIGN KEY (parent_item_id) REFERENCES item(id);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;

ALTER TABLE public.abe_component ADD CONSTRAINT fk1_abe_component FOREIGN KEY (item_id) REFERENCES public.item (id) ON DELETE CASCADE;

--
-- PostgreSQL database dump complete
--

