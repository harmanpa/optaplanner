/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.core.impl.score.buildin.hardmediumsoftbigdecimal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.optaplanner.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import org.optaplanner.core.impl.score.inliner.BigDecimalWeightedScoreImpacter;
import org.optaplanner.core.impl.score.inliner.JustificationsSupplier;
import org.optaplanner.core.impl.score.inliner.UndoScoreImpacter;

public class HardMediumSoftBigDecimalScoreInlinerTest {

    @Test
    public void buildWeightedScoreImpacter() {
        boolean constraintMatchEnabled = false;
        JustificationsSupplier justificationsSupplier = null;

        HardMediumSoftBigDecimalScoreInliner scoreInliner = new HardMediumSoftBigDecimalScoreInliner(constraintMatchEnabled);
        assertThat(scoreInliner.extractScore(0)).isEqualTo(HardMediumSoftBigDecimalScore.ZERO);

        BigDecimalWeightedScoreImpacter hardImpacter = scoreInliner
                .buildWeightedScoreImpacter("constraintPackage", "constraintName",
                        HardMediumSoftBigDecimalScore.ofHard(new BigDecimal("90.0")));
        UndoScoreImpacter hardUndo = hardImpacter.impactScore(new BigDecimal("1.0"), justificationsSupplier);
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(new BigDecimal("90.0"), BigDecimal.ZERO, BigDecimal.ZERO));
        scoreInliner
                .buildWeightedScoreImpacter("constraintPackage", "constraintName",
                        HardMediumSoftBigDecimalScore.ofHard(new BigDecimal("800.0")))
                .impactScore(new BigDecimal("1.0"), justificationsSupplier);
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(new BigDecimal("890.0"), BigDecimal.ZERO, BigDecimal.ZERO));
        hardUndo.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(new BigDecimal("800.0"), BigDecimal.ZERO, BigDecimal.ZERO));

        BigDecimalWeightedScoreImpacter mediumImpacter = scoreInliner
                .buildWeightedScoreImpacter("constraintPackage", "constraintName",
                        HardMediumSoftBigDecimalScore.ofMedium(new BigDecimal("7.0")));
        UndoScoreImpacter mediumUndo = mediumImpacter.impactScore(new BigDecimal("1.0"), justificationsSupplier);
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(new BigDecimal("800.0"), new BigDecimal("7.0"), BigDecimal.ZERO));
        mediumUndo.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(new BigDecimal("800.0"), BigDecimal.ZERO, BigDecimal.ZERO));

        BigDecimalWeightedScoreImpacter softImpacter = scoreInliner
                .buildWeightedScoreImpacter("constraintPackage", "constraintName",
                        HardMediumSoftBigDecimalScore.ofSoft(new BigDecimal("1.0")));
        UndoScoreImpacter softUndo = softImpacter.impactScore(new BigDecimal("3.0"), justificationsSupplier);
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(new BigDecimal("800.0"), BigDecimal.ZERO, new BigDecimal("3.0")));
        softImpacter.impactScore(new BigDecimal("10.0"), justificationsSupplier);
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(new BigDecimal("800.0"), BigDecimal.ZERO, new BigDecimal("13.0")));
        softUndo.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(new BigDecimal("800.0"), BigDecimal.ZERO, new BigDecimal("10.0")));

        BigDecimalWeightedScoreImpacter allLevelsImpacter = scoreInliner.buildWeightedScoreImpacter(
                "constraintPackage", "constraintName",
                HardMediumSoftBigDecimalScore.of(new BigDecimal("1000.0"), new BigDecimal("2000.0"), new BigDecimal("3000.0")));
        UndoScoreImpacter allLevelsUndo = allLevelsImpacter.impactScore(new BigDecimal("1.0"), justificationsSupplier);
        assertThat(scoreInliner.extractScore(0)).isEqualTo(
                HardMediumSoftBigDecimalScore.of(new BigDecimal("1800.0"), new BigDecimal("2000.0"), new BigDecimal("3010.0")));
        allLevelsUndo.run();
        assertThat(scoreInliner.extractScore(0))
                .isEqualTo(HardMediumSoftBigDecimalScore.of(new BigDecimal("800.0"), BigDecimal.ZERO, new BigDecimal("10.0")));
    }

}
