ALTER TABLE invitation_token
    DROP CONSTRAINT invitation_token_account_id_fkey,
    ADD CONSTRAINT invitation_token_account_id_fkey
        FOREIGN KEY (account_id) REFERENCES account(account_id) ON DELETE CASCADE;
