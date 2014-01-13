--
-- PostgreSQL database dump
--

-- Dumped from database version 9.2.3
-- Dumped by pg_dump version 9.2.3
-- Started on 2013-02-14 09:30:45 CET

DROP TABLE IF EXISTS public.object_version_chunk, public.chunk, public.object_version, public.object, public.workspace_user, public.workspace, public.device, public.user1 CASCADE;
DROP SEQUENCE IF EXISTS public.sequencer_user, public.sequencer_workspace, public.sequencer_device, public.sequencer_object, public.sequencer_object_version, public.sequencer_chunk;

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;


--
-- TOC entry 174 (class 3079 OID 11769)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

-- CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 2008 (class 0 OID 0)
-- Dependencies: 174
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

-- COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


-- SET search_path = public, pg_catalog;

SET default_tablespace = '';
SET default_with_oids = false;


CREATE SEQUENCE public.sequencer_user
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
    
CREATE SEQUENCE public.sequencer_workspace
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.sequencer_device
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.sequencer_object
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
    
CREATE SEQUENCE public.sequencer_object_version
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
    
--
-- TABLE: user
--

CREATE TABLE public.user1 (
    id bigint NOT NULL,
    name varchar(100) NOT NULL,
    cloud_id varchar(100) NOT NULL UNIQUE,
    email character varying(100) NOT NULL,
    quota_limit integer NOT NULL,
    quota_used integer DEFAULT 0 NOT NULL
);

ALTER TABLE public.user1 ADD CONSTRAINT pk_user PRIMARY KEY (id);

ALTER SEQUENCE public.sequencer_user OWNED BY public.user1.id;
ALTER TABLE ONLY public.user1 ALTER COLUMN id SET DEFAULT nextval('sequencer_user'::regclass);


--
-- TABLE: device
--

CREATE TABLE public.device (
    id bigint NOT NULL,
    name varchar(100) NOT NULL,
    user_id bigint NOT NULL,
    os varchar(100) NOT NULL,
    created_at timestamp,
    last_access_at timestamp,
    last_ip inet,
    app_version varchar(45)
);

ALTER TABLE public.device ADD CONSTRAINT pk_device PRIMARY KEY (id);

ALTER SEQUENCE public.sequencer_device OWNED BY public.device.id;
ALTER TABLE ONLY public.device ALTER COLUMN id SET DEFAULT nextval('sequencer_device'::regclass);
ALTER TABLE public.device ADD CONSTRAINT fk1_device FOREIGN KEY (user_id) REFERENCES public.user1 (id) ON DELETE CASCADE;


--
-- TABLE: workspace
--

CREATE TABLE public.workspace (
    id bigint NOT NULL,
    client_workspace_name varchar(45) UNIQUE,
    latest_revision varchar(45) NOT NULL DEFAULT 0,
    owner_id bigint NOT NULL
);

ALTER TABLE public.workspace ADD CONSTRAINT pk_workspace PRIMARY KEY (id);

ALTER SEQUENCE public.sequencer_workspace OWNED BY public.workspace.id;
ALTER TABLE ONLY public.workspace ALTER COLUMN id SET DEFAULT nextval('sequencer_workspace'::regclass);
ALTER TABLE public.workspace ADD CONSTRAINT fk1_workspace FOREIGN KEY (owner_id) REFERENCES public.user1 (id) ON DELETE CASCADE;


--
-- TABLE: workspace_user
--

CREATE TABLE public.workspace_user (
    workspace_id bigint NOT NULL,
    user_id bigint NOT NULL,
    client_workspace_path varchar(255) NOT NULL
);

ALTER TABLE public.workspace_user ADD CONSTRAINT pk_workspace_user PRIMARY KEY (workspace_id, user_id);
ALTER TABLE public.workspace_user ADD CONSTRAINT fk1_workspace_user FOREIGN KEY (user_id) REFERENCES public.user1 (id) ON DELETE CASCADE;
ALTER TABLE public.workspace_user ADD CONSTRAINT fk2_workspace_user FOREIGN KEY (workspace_id) REFERENCES public.workspace (id) ON DELETE CASCADE;




--
-- TABLE: object
--

CREATE TABLE public.object (
    id bigint NOT NULL,
    root_id varchar(45) NOT NULL,
    workspace_id bigint NOT NULL,
    latest_version bigint NOT NULL,
    parent_id bigint,
    client_file_id bigint NOT NULL,
    client_file_name varchar(100) NOT NULL,
    client_file_mimetype varchar(45) NOT NULL,
    client_folder boolean NOT NULL,
    client_parent_root_id varchar(45),
    client_parent_file_id bigint,
    client_parent_file_version bigint
);

ALTER TABLE public.object ADD CONSTRAINT pk_object PRIMARY KEY (id);

ALTER SEQUENCE public.sequencer_object OWNED BY public.object.id;
ALTER TABLE ONLY public.object ALTER COLUMN id SET DEFAULT nextval('sequencer_object'::regclass);
ALTER TABLE public.object ADD CONSTRAINT fk1_object FOREIGN KEY (workspace_id) REFERENCES public.workspace (id) ON DELETE CASCADE;
ALTER TABLE public.object ADD CONSTRAINT fk2_object FOREIGN KEY (parent_id) REFERENCES public.object (id) ON DELETE CASCADE;

CREATE INDEX object_workspace_id ON public.object (workspace_id);
CREATE INDEX object_file_id ON public.object (client_file_id);
CREATE INDEX object_parent_id ON public.object (parent_id);



--
-- TABLE: object_version
--

CREATE TABLE public.object_version (
    id bigint NOT NULL,
    object_id bigint NOT NULL,
    device_id bigint NOT NULL,
    version integer NOT NULL,
    modified timestamp,
    client_checksum bigint NOT NULL,
    client_mtime timestamp,
    client_status varchar(10) NOT NULL, --- TODO: mirar si postgres tiene tipo enumerado
    client_file_size bigint NOT NULL,
    client_name varchar(45) NOT NULL, --- TODO: info implicita en el device
    client_path varchar(100) NOT NULL --- TODO: mirar si se puede eliminar
);

ALTER TABLE public.object_version ADD CONSTRAINT pk_object_version PRIMARY KEY (id);
ALTER SEQUENCE public.sequencer_object_version OWNED BY public.object_version.id;
ALTER TABLE ONLY public.object_version ALTER COLUMN id SET DEFAULT nextval('sequencer_object_version'::regclass);

ALTER TABLE public.object_version ADD CONSTRAINT fk2_object_version FOREIGN KEY (object_id) REFERENCES public.object (id) ON DELETE CASCADE;
ALTER TABLE public.object_version ADD CONSTRAINT fk3_object_version FOREIGN KEY (device_id) REFERENCES public.device (id) ON DELETE CASCADE;

CREATE INDEX object_version_object_id ON public.object_version(object_id);

CREATE UNIQUE INDEX objectid_version_unique ON public.object_version (object_id, version);


--
-- TABLE: object_version_chunk
--

CREATE TABLE public.object_version_chunk (
    object_version_id bigint NOT NULL,	--- TODO: alomejor poniendo object_id y version podriamos ir mas rapido.???
	client_chunk_name character varying(40) NOT NULL,
    chunk_order integer NOT NULL
);

ALTER TABLE public.object_version_chunk ADD CONSTRAINT pk_object_version_chunk PRIMARY KEY (object_version_id, client_chunk_name, chunk_order);
ALTER TABLE public.object_version_chunk ADD CONSTRAINT fk2_object_version_chunk FOREIGN KEY (object_version_id) REFERENCES public.object_version (id) ON DELETE CASCADE;



--
-- FUNCTIONS
--

-- Returns the path given a client_file_id
CREATE OR REPLACE FUNCTION get_path(bigint, OUT result text)
  RETURNS text AS
$BODY$
BEGIN

	WITH RECURSIVE q AS 
	( 
		SELECT o.id, o.parent_id, ARRAY[o.id] AS level_array, '/' AS path
		FROM object o 
		WHERE o.id = $1
		UNION ALL 
		SELECT o2.id, o2.parent_id, q.level_array || o2.id,  '/' || o2.client_file_name::TEXT || q.path
		FROM q 
		JOIN object o2 ON o2.id = q.parent_id 
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

  
-- Returns an array of chunks corresponding to the given object_version_id
CREATE OR REPLACE FUNCTION get_chunks(bigint, OUT result text[])
  RETURNS text[] AS
$BODY$
BEGIN

--array_cat: appends an empty array to avoid null values when no chunks are found

    SELECT INTO result array_cat(ARRAY[]::character varying[], array_agg(client_chunk_name)) AS chunks
    FROM
    (
        SELECT ovc.client_chunk_name
        FROM object_version_chunk ovc
        WHERE ovc.object_version_id = $1
        ORDER BY ovc.chunk_order ASC
    ) AS foo;

END;
$BODY$
  LANGUAGE plpgsql VOLATILE;

