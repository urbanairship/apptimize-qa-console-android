package com.apptimize.qaconsole;

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
        Collections.sort(this.winners);

        for (Experiment exp : experiments) {
            // feature flag is the only type of experiment that has only one variant
            if (exp.getVariants().size() == 1) {
                featureFlags.add(exp);
            } else {
                runningExperiments.add(exp);
            }
        }
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
