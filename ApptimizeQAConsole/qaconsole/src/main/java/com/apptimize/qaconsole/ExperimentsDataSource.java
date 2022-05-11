package com.apptimize.qaconsole;

import android.util.Log;

import com.apptimize.ApptimizeInstantUpdateOrWinnerInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExperimentsDataSource {
    private List<Experiment> runningExperiments = new ArrayList<>();
    private List<Experiment> featureFlags = new ArrayList<>();
    private List<Experiment> winners = new ArrayList<>();

    ExperimentsDataSource(Map<Long, Map<String, Object>> variants,
                          Collection<ApptimizeInstantUpdateOrWinnerInfo> winners,
                          List<Long> knownWinnerExperiments) {

        for (ApptimizeInstantUpdateOrWinnerInfo winner : winners) {
            if (winner.getType() == ApptimizeInstantUpdateOrWinnerInfo.Type.INSTANT_UPDATE) {
                // we cannot force an instant update
                continue;
            }

            Experiment experiment = new Experiment(winner.getWinningTestName(),
                    winner.getWinningTestId());
            Variant variant = new Variant(winner.getWinningVariantName(),
                    winner.getWinningVariantId());
            variant.setChecked(true);
            experiment.addVariant(variant);
            this.winners.add(experiment);
        }

        List<Experiment> experiments = new ArrayList<>();

        for (Map<String, Object> source : variants.values()){
            Experiment experiment = new Experiment(source);

            // we can uncheck a winner. in this case it will not come in winners
            // but it will come as a variant
            if (knownWinnerExperiments.contains(experiment.id)) {
                if (this.winners.contains(experiment)) {
                    continue;
                }
                this.winners.add(experiment);
            } else {
                int index = experiments.indexOf(experiment);
                if (index < 0) {
                    experiments.add(experiment);
                } else {
                    experiment = experiments.get(index);
                }
            }

            experiment.addVariant(new Variant(source));
        }

        Collections.sort(experiments);

        for (Experiment exp : experiments) {
            // There are two types of experiments that has 1 variant:
            // 1. FeatureFlag: the variant name must be On State
            // 2. A winner that doesn't go because of pilot group \ user mismatch
            
            // Caveat: if user has created an A\B experiment with On State variant name and made this
            // variant as a winner and assign it to a particular pilog group \ user the other users
            // will see this experiment as a FeatureFlag instead of a winner.
            if (exp.getVariants().size() == 1) {
                if (exp.isFeatureFlag()) {
                    featureFlags.add(exp);
                } else {
                    this.winners.add(exp);
                }
            }
            else {
                runningExperiments.add(exp);
            }
        }

        Collections.sort(this.winners);
    }

    public List<Experiment> getRunningExperiments() {
        return Collections.unmodifiableList(runningExperiments);
    }

    public List<Experiment> getFeatureFlags() {
        return Collections.unmodifiableList(featureFlags);
    }

    public List<Experiment> getWinners() {
        return Collections.unmodifiableList(winners);
    }

    public List<Experiment> getAllExperiments() {
        List<Experiment> result = new ArrayList<Experiment>();
        result.addAll(runningExperiments);
        result.addAll(featureFlags);
        result.addAll(winners);
        return result;
    }
}
