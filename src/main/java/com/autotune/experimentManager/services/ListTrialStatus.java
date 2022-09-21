/*******************************************************************************
 * Copyright (c) 2021, 2021 Red Hat, IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.autotune.experimentManager.services;

import com.autotune.common.experiments.ExperimentTrial;
import com.autotune.common.experiments.ExperimentTrialView;
import com.autotune.common.experiments.TrialDetails;
import com.autotune.experimentManager.data.ExperimentDetailsMap;
import com.autotune.experimentManager.data.result.TrialMetaData;
import com.autotune.experimentManager.utils.EMConstants;
import com.google.gson.Gson;
import org.json.JSONObject;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import static com.autotune.utils.AnalyzerConstants.ServiceConstants.CHARACTER_ENCODING;
import static com.autotune.utils.AnalyzerConstants.ServiceConstants.JSON_CONTENT_TYPE;

/**
 * This class is the handler for the endpoint `listTrialStatus`
 *
 * Returns the status for the requested trial id
 */
public class ListTrialStatus extends HttpServlet {
    private static final long serialVersionUID = 1L;
    ExperimentDetailsMap<String, ExperimentTrial> existingExperiments;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.existingExperiments = (ExperimentDetailsMap<String, ExperimentTrial>) getServletContext().getAttribute(EMConstants.EMKeys.EM_STORAGE_CONTEXT_KEY);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String experiment_name = null;
        String trial_num = null;
        // Setting off verbose to false by default
        boolean verbose = false;

        // Extract Experiment Name if exist
        experiment_name = req.getParameter(EMConstants.InputJsonKeys.ListTrialStatusKeys.EXPERIMENT_NAME);
        // Extract Trial Number if exist
        trial_num = req.getParameter(EMConstants.InputJsonKeys.ListTrialStatusKeys.TRIAL_NUM);
        // Extract Verbose option if exist
        String checkVerbose = req.getParameter(EMConstants.InputJsonKeys.ListTrialStatusKeys.VERBOSE);
        if (null != checkVerbose && checkVerbose.equalsIgnoreCase("true")) {
            verbose = true;
        }

        resp.setContentType(JSON_CONTENT_TYPE);
        resp.setCharacterEncoding(CHARACTER_ENCODING);
        resp.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = resp.getWriter();
        JSONObject returnJson = new JSONObject();

        if (null == experiment_name){
            this.existingExperiments.forEach((expName,experimentTObj)->{
                JSONObject trialJson = new JSONObject();
            /*    ((ExperimentTrial) experimentTObj).getTrialDetails().forEach((trialNum,trialDetail)->{
                    trialJson.put(trialNum,new JSONObject().put("STATUS" , "COMPLETED"));
                });*/
                returnJson.put((String) expName,experimentTObj);
            });
        }else{
            ExperimentTrial et = (ExperimentTrial) this.existingExperiments.get(experiment_name);
            if (null != et) {
                JSONObject trialJson = new JSONObject();
                if (null != trial_num){
                    TrialDetails td = et.getTrialDetails().get(trial_num);
                    if(null != td) {
                        trialJson.put(trial_num, new JSONObject().put("STATUS", "COMPLETED"));
                    }
                }else {
                    et.getTrialDetails().forEach((trialNum, trialDetail) -> {
                        trialJson.put(trialNum, new JSONObject().put("STATUS", "COMPLETED"));
                    });
                }
                returnJson.put(experiment_name,trialJson);
            }
        }
        resp.setContentType(JSON_CONTENT_TYPE);
        resp.setCharacterEncoding(CHARACTER_ENCODING);
        resp.setStatus(HttpServletResponse.SC_CREATED);
        HashMap<String,ExperimentTrialView> experimentTrialViews = new HashMap<>();
        this.existingExperiments.forEach((expName,experimentObj)->{
            ExperimentTrial eObj = (ExperimentTrial) experimentObj;
            ExperimentTrialView experimentTrialView = new ExperimentTrialView();
            experimentTrialView.setStatus(eObj.getStatus());
            experimentTrialView.setCreationDate(eObj.getExperimentMetaData().getCreationDate());
            experimentTrialView.setBeginTimeStamp(eObj.getExperimentMetaData().getBeginTimestamp());
            experimentTrialView.setEndTimeStamp(eObj.getExperimentMetaData().getEndTimestamp());
            LinkedList<String> steps=  new LinkedList<>();
            steps.addAll(eObj.getExperimentMetaData().getAutoTuneWorkFlow().getIterationWorkflowMap().keySet());
            experimentTrialView.setSteps(steps);
            LinkedHashMap<String, TrialMetaData> trialMetaDataLinkedHashMap = new LinkedHashMap<>();
            eObj.getTrialDetails().forEach((trialNum,trialDetails)->{
                trialMetaDataLinkedHashMap.put(trialNum,trialDetails.getTrialMetaData());
            });
            experimentTrialView.setTrialDetails(trialMetaDataLinkedHashMap);
            experimentTrialViews.put(eObj.getExperimentName(), experimentTrialView);
        });
        out = resp.getWriter();
        out.append(
                new Gson().toJson(
                        experimentTrialViews
                )
        );
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }
}
