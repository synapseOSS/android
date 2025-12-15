-- Create function to trigger mention handler
CREATE OR REPLACE FUNCTION trigger_mention_handler()
RETURNS TRIGGER AS $$
BEGIN
  -- Check if content contains @syra mention
  IF (NEW.content ~* '@syra' OR NEW.message_text ~* '@syra') THEN
    -- Call the mention trigger function
    PERFORM net.http_post(
      url := current_setting('app.supabase_url') || '/functions/v1/mention-trigger',
      headers := jsonb_build_object(
        'Authorization', 'Bearer ' || current_setting('app.service_role_key'),
        'Content-Type', 'application/json'
      ),
      body := jsonb_build_object(
        'record', row_to_json(NEW),
        'type', TG_TABLE_NAME
      )
    );
  END IF;
  
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for messages table
DROP TRIGGER IF EXISTS mention_trigger_messages ON messages;
CREATE TRIGGER mention_trigger_messages
  AFTER INSERT ON messages
  FOR EACH ROW
  EXECUTE FUNCTION trigger_mention_handler();

-- Create triggers for comments table  
DROP TRIGGER IF EXISTS mention_trigger_comments ON comments;
CREATE TRIGGER mention_trigger_comments
  AFTER INSERT ON comments
  FOR EACH ROW
  EXECUTE FUNCTION trigger_mention_handler();

-- Create triggers for posts table
DROP TRIGGER IF EXISTS mention_trigger_posts ON posts;
CREATE TRIGGER mention_trigger_posts
  AFTER INSERT ON posts
  FOR EACH ROW
  EXECUTE FUNCTION trigger_mention_handler();
