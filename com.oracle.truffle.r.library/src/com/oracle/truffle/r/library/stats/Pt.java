/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995, 1996, Robert Gentleman and Ross Ihaka
 * Copyright (c) 2000-2007, The R Core Team
 * Copyright (c) 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.stats;

import static com.oracle.truffle.r.library.stats.GammaFunctions.pnorm;
import static com.oracle.truffle.r.library.stats.LBeta.lbeta;
import static com.oracle.truffle.r.library.stats.MathConstants.M_LN2;
import static com.oracle.truffle.r.library.stats.Pbeta.pbeta;

import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.library.stats.StatsFunctions.Function2_2;

public class Pt implements Function2_2 {
    private final BranchProfile pbetaNanProfile = BranchProfile.create();

    @Override
    public double evaluate(double x, double n, boolean lowerTail, boolean logP) {
        /*
         * return P[ T <= x ] where T ~ t_{n} (t distrib. with n degrees of freedom).
         *
         * --> ./pnt.c for NON-central
         */
        if (Double.isNaN(x) || Double.isNaN(n)) {
            return x + n;
        }

        if (n <= 0.0) {
            return RMath.mlError();
        }

        if (!Double.isFinite(x)) {
            return (x < 0) ? DPQ.rdt0(lowerTail, logP) : DPQ.rdt1(lowerTail, logP);
        }
        if (!Double.isFinite(n)) {
            return pnorm(x, 0.0, 1.0, lowerTail, logP);
        }

        double nx = 1 + (x / n) * x;
        /*
         * FIXME: This test is probably losing rather than gaining precision, now that pbeta(*,
         * log_p = true) is much better. Note however that a version of this test *is* needed for
         * x*x > D_MAX
         */
        double val;
        if (nx > 1e100) { /* <==> x*x > 1e100 * n */
            /*
             * Danger of underflow. So use Abramowitz & Stegun 26.5.4 pbeta(z, a, b) ~ z^a(1-z)^b /
             * aB(a,b) ~ z^a / aB(a,b), with z = 1/nx, a = n/2, b= 1/2 :
             */
            double lval;
            lval = -0.5 * n * (2 * Math.log(Math.abs(x)) - Math.log(n)) - lbeta(0.5 * n, 0.5) - Math.log(0.5 * n);
            val = logP ? lval : Math.exp(lval);
        } else {
            val = (n > x * x)
                            ? pbeta(x * x / (n + x * x), 0.5, n / 2., /* lower_tail */false, logP, pbetaNanProfile)
                            : pbeta(1. / nx, n / 2., 0.5, /* lower_tail */true, logP, pbetaNanProfile);
        }

        /* Use "1 - v" if lower_tail and x > 0 (but not both): */
        if (x <= 0.) {
            lowerTail = !lowerTail;
        }

        if (logP) {
            if (lowerTail) {
                return RMath.log1p(-0.5 * Math.exp(val));
            } else {
                return val - M_LN2; /* = Math.log(.5* pbeta(....)) */
            }
        } else {
            val /= 2.;
            return DPQ.rdcval(val, lowerTail);
        }

    }
}