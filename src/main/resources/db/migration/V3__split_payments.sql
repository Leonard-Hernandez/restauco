-- A bill can be paid with several methods (card 5.000 + cash 2.000). Each part keeps a
-- copy of the method name, like payment_method_name, so renaming a method never rewrites
-- past bills. payment_method_id/name stay as the main method and a readable summary.
ALTER TABLE table_session ADD COLUMN payments JSONB NOT NULL DEFAULT '[]';

UPDATE table_session
SET payments = jsonb_build_array(jsonb_build_object(
        'methodId', payment_method_id, 'name', payment_method_name, 'amount', total))
WHERE status = 'CLOSED' AND payment_method_id IS NOT NULL;
