package se.kth.castor.jdbl.agent;

import net.bytebuddy.asm.Advice;

class CallAdvice {
    @Advice.OnMethodEnter()
    static void enter(@Advice.Origin("#t,#m,#d") String origin) {
        Agent.calledMethods.add(origin);
    }
}