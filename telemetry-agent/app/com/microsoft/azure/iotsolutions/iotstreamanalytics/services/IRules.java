// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.google.inject.ImplementedBy;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.RuleApiModel;

import java.util.ArrayList;
import java.util.concurrent.CompletionStage;

@ImplementedBy(Rules.class)
public interface IRules {
    CompletionStage<ArrayList<RuleApiModel>> getAllAsync();
}
