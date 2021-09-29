# --- !Ups
create extension if not exists "uuid-ossp";

create table tags
(
    tag_id uuid primary key,
    tag_text text not null
);

create table active_users
(
    user_id uuid primary key
);

create table question_user
(
    question_id uuid primary key,
    user_id uuid not null
);

create table tag_question
(
    tag_id uuid not null,
    question_id uuid not null,
    foreign key (tag_id) references tags(tag_id) on delete cascade,
    foreign key (question_id) references question_user(question_id) on delete cascade,
    primary key (tag_id, question_id)
);

# --- !Downs
drop table tag_question;
drop table question_user;
drop table active_users;
drop table tags;
drop extension "uuid-ossp"