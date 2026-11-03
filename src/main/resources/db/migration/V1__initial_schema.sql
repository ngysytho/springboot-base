CREATE TABLE organizations (id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, version bigint NOT NULL DEFAULT 0, name varchar(200) NOT NULL);
CREATE TABLE user_profiles (id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, version bigint NOT NULL DEFAULT 0, subject varchar(255) NOT NULL UNIQUE, display_name varchar(255) NOT NULL);
CREATE TABLE organization_memberships (id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, version bigint NOT NULL DEFAULT 0,
 organization_id uuid NOT NULL REFERENCES organizations(id),
 user_profile_id uuid NOT NULL REFERENCES user_profiles(id),
 role varchar(255) NOT NULL CHECK (role IN ('ORG_ADMIN','TEACHER','STUDENT','PARENT','BUSINESS_MANAGER')),
 active boolean NOT NULL DEFAULT true,
 UNIQUE (organization_id,user_profile_id));
CREATE TABLE lessons (id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, version bigint NOT NULL DEFAULT 0,
 organization_id uuid NOT NULL REFERENCES organizations(id),
 title varchar(200) NOT NULL CHECK (length(trim(title))>0), description varchar(10000),
 status varchar(20) NOT NULL CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED')));
CREATE INDEX ix_lessons_org_created_id ON lessons(organization_id,created_at,id);
CREATE INDEX ix_memberships_user ON organization_memberships(user_profile_id);
