--
-- PostgreSQL database dump
--

-- Dumped from database version 9.2.3
-- Dumped by pg_dump version 9.2.3
-- Started on 2013-02-14 09:30:45 CET

DROP TABLE IF EXISTS public.item_version_chunk, public.chunk, public.item_version, public.item, public.workspace_user, public.workspace, public.device, public.user1 CASCADE;
DROP SEQUENCE IF EXISTS public.sequencer_user, public.sequencer_workspace, public.sequencer_device, public.sequencer_item, public.sequencer_item_version, public.sequencer_chunk;

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
-- TABLE: item
--

CREATE TABLE public.item (
    id bigint NOT NULL,
    workspace_id bigint NOT NULL,
    latest_version bigint NOT NULL,
    parent_id bigint,
    filename varchar(100) NOT NULL,
    mimetype varchar(45) NOT NULL,
    is_folder boolean NOT NULL,
    client_parent_file_version bigint
);

ALTER TABLE public.item ADD CONSTRAINT pk_item PRIMARY KEY (id);

ALTER SEQUENCE public.sequencer_item OWNED BY public.item.id;
ALTER TABLE ONLY public.item ALTER COLUMN id SET DEFAULT nextval('sequencer_item'::regclass);
ALTER TABLE public.item ADD CONSTRAINT fk1_item FOREIGN KEY (workspace_id) REFERENCES public.workspace (id) ON DELETE CASCADE;
ALTER TABLE public.item ADD CONSTRAINT fk2_item FOREIGN KEY (parent_id) REFERENCES public.item (id) ON DELETE CASCADE;

CREATE INDEX item_workspace_id ON public.item (workspace_id);
CREATE INDEX item_parent_id ON public.item (parent_id);



--
-- TABLE: item_version
--

CREATE TABLE public.item_version (
    id bigint NOT NULL,
    item_id bigint NOT NULL,
    device_id bigint NOT NULL,
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
    item_version_id bigint NOT NULL,	--- TODO: alomejor poniendo item_id y version podriamos ir mas rapido.???
	client_chunk_name character varying(40) NOT NULL,
    chunk_order integer NOT NULL
);

ALTER TABLE public.item_version_chunk ADD CONSTRAINT pk_item_version_chunk PRIMARY KEY (item_version_id, client_chunk_name, chunk_order);
ALTER TABLE public.item_version_chunk ADD CONSTRAINT fk2_item_version_chunk FOREIGN KEY (item_version_id) REFERENCES public.item_version (id) ON DELETE CASCADE;



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

