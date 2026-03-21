-- 특정 이메일을 가진 유저를 CASCADE로 삭제하는 스크립트
-- 이메일: tldms0507@naver.com

BEGIN;

-- 삭제할 유저 ID 확인
DO $$
DECLARE
    target_user_id BIGINT;
    deleted_count INTEGER;
BEGIN
    -- 유저 ID 조회
    SELECT id INTO target_user_id
    FROM users
    WHERE email = 'tldms0507@naver.com';
    
    IF target_user_id IS NULL THEN
        RAISE NOTICE '유저를 찾을 수 없습니다: tldms0507@naver.com';
        RETURN;
    END IF;
    
    RAISE NOTICE '삭제 대상 유저 ID: %', target_user_id;
    
    -- 1. practice_sessions 삭제 (userId로 연결)
    DELETE FROM practice_sessions WHERE user_id = target_user_id;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'practice_sessions 삭제: % 개', deleted_count;
    
    -- 2. practice_routine_sequence 삭제 (practice_routines를 통해)
    DELETE FROM practice_routine_sequence
    WHERE routine_id IN (
        SELECT id FROM practice_routines WHERE user_id = target_user_id
    );
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'practice_routine_sequence 삭제: % 개', deleted_count;
    
    -- 3. practice_routines 삭제 (userId로 연결)
    DELETE FROM practice_routines WHERE user_id = target_user_id;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'practice_routines 삭제: % 개', deleted_count;
    
    -- 4. user_status 삭제 (user_id로 연결)
    DELETE FROM user_status WHERE user_id = target_user_id;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'user_status 삭제: % 개', deleted_count;
    
    -- 5. users 삭제 (최종)
    DELETE FROM users WHERE id = target_user_id;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RAISE NOTICE 'users 삭제: % 개', deleted_count;
    
    RAISE NOTICE '유저 삭제 완료: tldms0507@naver.com';
END $$;

-- 삭제 확인
SELECT 
    'users' as table_name,
    COUNT(*) as remaining_count
FROM users
WHERE email = 'tldms0507@naver.com'
UNION ALL
SELECT 
    'user_status' as table_name,
    COUNT(*) as remaining_count
FROM user_status us
JOIN users u ON us.user_id = u.id
WHERE u.email = 'tldms0507@naver.com'
UNION ALL
SELECT 
    'practice_routines' as table_name,
    COUNT(*) as remaining_count
FROM practice_routines pr
JOIN users u ON pr.user_id = u.id
WHERE u.email = 'tldms0507@naver.com'
UNION ALL
SELECT 
    'practice_sessions' as table_name,
    COUNT(*) as remaining_count
FROM practice_sessions ps
JOIN users u ON ps.user_id = u.id
WHERE u.email = 'tldms0507@naver.com';

-- 트랜잭션 커밋 (모든 삭제가 성공하면)
COMMIT;

-- 롤백이 필요한 경우:
-- ROLLBACK;

