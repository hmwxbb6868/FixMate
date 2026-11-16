UPDATE pricing_rule
SET fixed_price = 50.00,
    min_price = NULL,
    max_price = NULL,
    version = version + 1,
    updated_at = UTC_TIMESTAMP(3)
WHERE fee_type = 'VISIT'
  AND status = 'ENABLED'
  AND (fixed_price IS NULL OR fixed_price <> 50.00 OR min_price IS NOT NULL OR max_price IS NOT NULL);
