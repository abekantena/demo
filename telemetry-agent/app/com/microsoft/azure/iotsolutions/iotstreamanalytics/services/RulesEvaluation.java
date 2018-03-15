// Copyright (c) Microsoft. All rights reserved.

package com.microsoft.azure.iotsolutions.iotstreamanalytics.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.exceptions.ExternalDependencyException;
import com.microsoft.azure.iotsolutions.iotstreamanalytics.services.models.*;
import org.joda.time.DateTime;
import play.Logger;
import play.libs.F;

import java.util.*;

// TODO: the class is loading rules too often
// https://github.com/Azure/telemetry-agent-java/issues/36
@Singleton
public class RulesEvaluation implements IRulesEvaluation {

    private static final Logger.ALogger log = Logger.of(RulesEvaluation.class);

    // For each group, reload the device IDs once every 5 minutes
    private static int GROUPS_CACHE_TTL_SECONDS = 300;

    // Groups cache: <Group ID, <list of devices, cache expiration>>
    private final HashMap<String, F.Tuple<Set<String>, DateTime>> deviceGroupsCache;

    private final IDeviceGroups deviceGroups;

    // Important! keep the values lowercase
    @SuppressWarnings("SpellCheckingInspection")
    private static final String GT1_OPERATOR = "greaterthan";
    private static final String GT2_OPERATOR = ">";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String GTE1_OPERATOR = "greaterthanorequal";
    private static final String GTE2_OPERATOR = ">=";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String LT1_OPERATOR = "lessthan";
    private static final String LT2_OPERATOR = "<";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String LTE1_OPERATOR = "lessthanorequal";
    private static final String LTE2_OPERATOR = "<=";
    private static final String EQ1_OPERATOR = "equals";
    private static final String EQ2_OPERATOR = "equal";
    private static final String EQ3_OPERATOR = "=";
    private static final String EQ4_OPERATOR = "==";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String NEQ1_OPERATOR = "notequal";
    @SuppressWarnings("SpellCheckingInspection")
    private static final String NEQ2_OPERATOR = "notequals";
    private static final String NEQ3_OPERATOR = "!=";
    private static final String NEQ4_OPERATOR = "<>";

    private final String GT_MESSAGE = "`%s` value `%s` is greater than `%s`";
    private final String GTE_MESSAGE = "`%s` value `%s` is greater than or equal to `%s`";
    private final String LT_MESSAGE = "`%s` value `%s` is less than `%s`";
    private final String LTE_MESSAGE = "`%s` value `%s` is less than or equal to `%s`";
    private final String EQ_MESSAGE = "`%s` value `%s` is equal to `%s`";
    private final String NEQ_MESSAGE = "`%s` value `%s` is not equal to `%s`";

    @Inject
    public RulesEvaluation(IDeviceGroups deviceGroups) {
        this.deviceGroups = deviceGroups;
        this.deviceGroupsCache = new HashMap<>();
    }

    public RulesEvaluationResult evaluate(RuleApiModel rule, RawMessage message)
        throws ExternalDependencyException {

        RulesEvaluationResult result = new RulesEvaluationResult();

        if (this.groupContainsDevice(message.getDeviceId(), rule.getGroupId())) {

            log.debug("Evaluating rule {} for device {} with {} conditions",
                rule.getDescription(), message.getDeviceId(), rule.getConditions().size());

            ArrayList<String> descriptions = new ArrayList<>();
            for (ConditionApiModel c : rule.getConditions()) {
                ConditionEvaluationResult eval = this.evaluateCondition(c, message);
                // perf: all conditions must match, break as soon as one doesn't
                if (!eval.match) return result;
                descriptions.add(eval.message);
            }

            result.match = true;
            result.message = String.join("; ", descriptions);
        } else {
            log.debug("Skipping rule {} because device {} doesn't belong to group {}",
                rule.getDescription(), message.getDeviceId(), rule.getGroupId());
        }

        return result;
    }

    private boolean groupContainsDevice(String deviceId, String groupId)
        throws ExternalDependencyException {

        synchronized (this.deviceGroupsCache) {
            // Check if the cache is expired
            if (this.deviceGroupsCache.containsKey(groupId)) {
                if (this.deviceGroupsCache.get(groupId)._2.isBeforeNow()) {
                    log.debug("Cache for group {} expired", groupId);
                    this.deviceGroupsCache.remove(groupId);
                }
            }

            // If the the group information is in not available, retrieve and cache
            if (!this.deviceGroupsCache.containsKey(groupId)) {
                log.debug("Preparing cache for group {}", groupId);

                this.deviceGroupsCache.put(
                    groupId,
                    new F.Tuple<>(
                        this.deviceGroups.getDevices(groupId),
                        DateTime.now().plusSeconds(GROUPS_CACHE_TTL_SECONDS)
                    ));
            }
        }

        // Check if the group contains the device ID
        return (this.deviceGroupsCache.get(groupId)._1.contains(deviceId));
    }

    private ConditionEvaluationResult evaluateCondition(
        ConditionApiModel condition,
        RawMessage message) {

        ConditionEvaluationResult r = new ConditionEvaluationResult();

        String field = condition.getField();

        if (!message.hasPayloadField(field)) {
            log.debug("Message payload doesn't contain field {}", field);
            return r;
        }

        if (message.payloadIsNumber(field)) {

            double actualValue = message.getNumberFromPayload(field);
            double threshold = Double.parseDouble(condition.getValue());
            String operator = condition.getOperator().toLowerCase();

            log.debug("Field {}, Value {}, Operator {}, Threshold {}", field, actualValue, operator, threshold);

            switch (operator) {
                case GT1_OPERATOR:
                case GT2_OPERATOR:
                    if (actualValue > threshold) {
                        r.match = true;
                        r.message = String.format(GT_MESSAGE, field, actualValue, threshold);
                    }
                    break;
                case GTE1_OPERATOR:
                case GTE2_OPERATOR:
                    if (actualValue >= threshold) {
                        r.match = true;
                        r.message = String.format(GTE_MESSAGE, field, actualValue, threshold);
                    }
                    break;
                case LT1_OPERATOR:
                case LT2_OPERATOR:
                    if (actualValue < threshold) {
                        r.match = true;
                        r.message = String.format(LT_MESSAGE, field, actualValue, threshold);
                    }
                    break;
                case LTE1_OPERATOR:
                case LTE2_OPERATOR:
                    if (actualValue <= threshold) {
                        r.match = true;
                        r.message = String.format(LTE_MESSAGE, field, actualValue, threshold);
                    }
                    break;
                case EQ1_OPERATOR:
                case EQ2_OPERATOR:
                case EQ3_OPERATOR:
                case EQ4_OPERATOR:
                    if (actualValue == threshold) {
                        r.match = true;
                        r.message = String.format(EQ_MESSAGE, field, actualValue, threshold);
                    }
                    break;
                case NEQ1_OPERATOR:
                case NEQ2_OPERATOR:
                case NEQ3_OPERATOR:
                case NEQ4_OPERATOR:
                    if (actualValue != threshold) {
                        r.match = true;
                        r.message = String.format(NEQ_MESSAGE, field, actualValue, threshold);
                    }
                    break;
            }
        } else if (message.payloadIsString(field)) {

            String actualValue = message.getStringFromPayload(field);
            String threshold = condition.getValue();
            String operator = condition.getOperator().toLowerCase();

            log.debug("Field {}, Value '{}', Operator {}, Threshold '{}'", field, actualValue, operator, threshold);

            switch (operator) {
                case GT1_OPERATOR:
                case GT2_OPERATOR:
                    if (actualValue.compareToIgnoreCase(threshold) > 0) {
                        r.match = true;
                        r.message = String.format(GT_MESSAGE, field, actualValue, threshold);
                    }
                    break;
                case GTE1_OPERATOR:
                case GTE2_OPERATOR:
                    if (actualValue.compareToIgnoreCase(threshold) >= 0) {
                        r.match = true;
                        r.message = String.format(GTE_MESSAGE, field, actualValue, threshold);
                    }
                    break;
                case LT1_OPERATOR:
                case LT2_OPERATOR:
                    if (actualValue.compareToIgnoreCase(threshold) < 0) {
                        r.match = true;
                        r.message = String.format(LT_MESSAGE, field, actualValue, threshold);
                    }
                    break;
                case LTE1_OPERATOR:
                case LTE2_OPERATOR:
                    if (actualValue.compareToIgnoreCase(threshold) <= 0) {
                        r.match = true;
                        r.message = String.format(LTE_MESSAGE, field, actualValue, threshold);
                    }
                    break;
                case EQ1_OPERATOR:
                case EQ2_OPERATOR:
                case EQ3_OPERATOR:
                case EQ4_OPERATOR:
                    if (actualValue.equalsIgnoreCase(threshold)) {
                        r.match = true;
                        r.message = String.format(EQ_MESSAGE, field, actualValue, threshold);
                    }
                    break;
                case NEQ1_OPERATOR:
                case NEQ2_OPERATOR:
                case NEQ3_OPERATOR:
                case NEQ4_OPERATOR:
                    if (!actualValue.equalsIgnoreCase(threshold)) {
                        r.match = true;
                        r.message = String.format(NEQ_MESSAGE, field, actualValue, threshold);
                    }
                    break;
            }
        } else {
            log.error("Unknown type for `{}` value sent by device {}", field, message.getDeviceId());
        }

        return r;
    }

    private class ConditionEvaluationResult {
        public boolean match = false;
        public String message = "";
    }
}
