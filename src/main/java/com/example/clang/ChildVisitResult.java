package com.example.clang;

import static org.bytedeco.llvm.global.clang.CXChildVisit_Break;
import static org.bytedeco.llvm.global.clang.CXChildVisit_Continue;
import static org.bytedeco.llvm.global.clang.CXChildVisit_Recurse;

@SuppressWarnings("ConstantConditions")
public enum ChildVisitResult {
    /**
     * @see org.bytedeco.llvm.global.clang#CXChildVisit_Break
     */
    BREAK,

    /**
     * @see org.bytedeco.llvm.global.clang#CXChildVisit_Continue
     */
    CONTINUE,

    /**
     * @see org.bytedeco.llvm.global.clang#CXChildVisit_Recurse
     */
    RECURSE,
    ;

    static {
        assert BREAK.ordinal() == CXChildVisit_Break;
        assert CONTINUE.ordinal() == CXChildVisit_Continue;
        assert RECURSE.ordinal() == CXChildVisit_Recurse;
    }
}
