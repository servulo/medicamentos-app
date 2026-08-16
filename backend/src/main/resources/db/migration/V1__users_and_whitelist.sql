CREATE TABLE users (
    id UUID PRIMARY KEY,
    google_sub VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE whitelist_entries (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_email VARCHAR(320) NOT NULL
);
