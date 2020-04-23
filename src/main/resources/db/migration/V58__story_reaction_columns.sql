ALTER TABLE stories ADD summary VARCHAR(512) NOT NULL DEFAULT '';
CREATE TABLE stories_reactions (
    story_id uuid NOT NULL,
    reaction_id uuid NOT NULL
);
ALTER TABLE stories_reactions ADD FOREIGN KEY (story_id) REFERENCES stories(id) ON UPDATE CASCADE ON DELETE CASCADE;
ALTER TABLE stories_reactions ADD FOREIGN KEY (reaction_id) REFERENCES reactions(id) ON UPDATE CASCADE ON DELETE CASCADE;
CREATE INDEX ON stories_reactions(story_id);
CREATE INDEX ON stories_reactions(reaction_id);
