package com.securecare.service;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class LoginAttemptServiceTest {
 @Test void allow_sixthAttemptAfterFiveFailures_expectedBlocked(){var limiter=new LoginAttemptService();for(int i=0;i<5;i++)limiter.recordFailure("127.0.0.1");assertFalse(limiter.allow("127.0.0.1"));}
 @Test void reset_afterSuccessfulLogin_expectedAllowed(){var limiter=new LoginAttemptService();for(int i=0;i<5;i++)limiter.recordFailure("127.0.0.1");limiter.reset("127.0.0.1");assertTrue(limiter.allow("127.0.0.1"));}
 @Test void allow_differentIp_expectedIndependentWindow(){var limiter=new LoginAttemptService();for(int i=0;i<5;i++)limiter.recordFailure("10.0.0.1");assertTrue(limiter.allow("10.0.0.2"));}
}
