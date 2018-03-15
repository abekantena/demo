// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.google.inject.ImplementedBy;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.exceptions.ExternalDependencyException;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.RawMessage;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.RuleApiModel;

@ImplementedBy(RulesEvaluation.class)
public interface IRulesEvaluation {

    class RulesEvaluationResult {
        public boolean match = false;
        public String message = "";
    }

    RulesEvaluation.RulesEvaluationResult evaluate(RuleApiModel rule, RawMessage message)
        throws ExternalDependencyException;
}
