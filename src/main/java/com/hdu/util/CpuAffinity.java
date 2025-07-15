//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.hdu.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Structure;
import java.util.Collections;
import java.util.List;

public class CpuAffinity {
    public CpuAffinity() {
    }

    public static void bindToCpu(int cpuId) {
        int pid = 0;
        cpu_set_t cpuset = new cpu_set_t();
        cpuset.CPU_ZERO();
        cpuset.CPU_SET(cpuId);
        int result = CpuAffinity.CLibrary.INSTANCE.sched_setaffinity(pid, cpuset.size(), cpuset);
        if (result != 0) {
            System.err.println("Failed to bind to CPU " + cpuId);
        } else {
            System.out.println("Thread " + Thread.currentThread().getName() + " bound to CPU " + cpuId);
        }

    }

    public interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary)Native.load(Platform.isWindows() ? "msvcrt" : "c", CLibrary.class);

        int sched_setaffinity(int var1, int var2, cpu_set_t var3);

        int sched_getcpu();
    }

    public static class cpu_set_t extends Structure {
        public static final int CPU_SETSIZE = 1024;
        public static final int __NCPUBITS = 64;
        public long[] __bits = new long[16];

        public cpu_set_t() {
        }

        protected List<String> getFieldOrder() {
            return Collections.singletonList("__bits");
        }

        public void CPU_ZERO() {
            for(int i = 0; i < this.__bits.length; ++i) {
                this.__bits[i] = 0L;
            }

        }

        public void CPU_SET(int cpu) {
            long[] var10000 = this.__bits;
            var10000[cpu / 64] |= 1L << cpu % 64;
        }
    }
}
