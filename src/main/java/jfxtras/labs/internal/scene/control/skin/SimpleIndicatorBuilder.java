/*
 * Copyright (c) 2012, JFXtras
 *   All rights reserved.
 *
 *   Redistribution and use in source and binary forms, with or without
 *   modification, are permitted provided that the following conditions are met:
 *       * Redistributions of source code must retain the above copyright
 *         notice, this list of conditions and the following disclaimer.
 *       * Redistributions in binary form must reproduce the above copyright
 *         notice, this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *       * Neither the name of the <organization> nor the
 *         names of its contributors may be used to endorse or promote products
 *         derived from this software without specific prior written permission.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *   ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 *   DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *   (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *   ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jfxtras.labs.internal.scene.control.skin;

import javafx.scene.paint.Color;
import jfxtras.labs.scene.control.gauge.SimpleIndicator;


/**
 * Created by
 * User: hansolo
 * Date: 09.03.12
 * Time: 16:04
 */
public class SimpleIndicatorBuilder {
    private SimpleIndicator indicator;

    public final SimpleIndicatorBuilder create() {
        indicator = new SimpleIndicator();
        return this;
    }

    public final SimpleIndicatorBuilder innerColor(final Color INNER_COLOR) {
        indicator.setInnerColor(INNER_COLOR);
        return this;
    }

    public final SimpleIndicatorBuilder outerColor(final Color OUTER_COLOR) {
        indicator.setOuterColor(OUTER_COLOR);
        return this;
    }

    public final SimpleIndicatorBuilder glowVisible(final boolean GLOW_VISIBLE) {
        indicator.setGlowVisible(GLOW_VISIBLE);
        return this;
    }

    public final SimpleIndicator build() {
        return indicator;
    }
}