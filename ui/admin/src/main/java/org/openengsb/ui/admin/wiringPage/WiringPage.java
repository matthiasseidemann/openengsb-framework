/**
 * Licensed to the Austrian Association for Software Tool Integration (AASTI)
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. The AASTI licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openengsb.ui.admin.wiringPage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.tree.BaseTree;
import org.apache.wicket.markup.html.tree.LabelTree;
import org.apache.wicket.markup.html.tree.LinkTree;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.value.ValueMap;
import org.openengsb.core.api.ConnectorManager;
import org.openengsb.core.api.Constants;
import org.openengsb.core.api.Domain;
import org.openengsb.core.api.DomainProvider;
import org.openengsb.core.api.OsgiUtilsService;
import org.openengsb.core.api.WiringService;
import org.openengsb.core.api.model.ConnectorDescription;
import org.openengsb.core.api.model.ConnectorId;
import org.openengsb.core.api.workflow.RuleManager;
import org.openengsb.core.common.util.Comparators;
import org.openengsb.ui.admin.basePage.BasePage;
import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AuthorizeInstantiation("ROLE_ADMIN")
public class WiringPage extends BasePage {
    private static final Logger LOGGER = LoggerFactory.getLogger(WiringPage.class);
    
    @SpringBean
    private WiringService wiringService;

    @SpringBean
    private OsgiUtilsService serviceUtils;
    
    @SpringBean
    private ConnectorManager serviceManager;
    
    @SpringBean
    private RuleManager ruleManager;
    
    private DropDownChoice<Class<? extends Domain>> domains;
    private LinkTree globals;
    private LinkTree endpoints;
    private TextField<String> txtGlobalName;
    private TextField<String> txtInstanceId;
    private CheckedTree contextList;
    private AjaxSubmitLink wireButton;
    private FeedbackPanel feedbackPanel;
    
    private String globalName = "";
    private String instanceId = "";
    
    public WiringPage() {
        init();
    }
    
    private void init() {
        Form<Void> domainChooseForm = new Form<Void>("domainChooseForm");
        initDomainChooseForm(domainChooseForm);
        add(domainChooseForm);
        
        globals = new WiringSubjectTree("globals");
        globals.getTreeState().expandAll();
        globals.setOutputMarkupId(true);
        globals.setOutputMarkupPlaceholderTag(true);
        add(globals);
        
        endpoints = new WiringSubjectTree("endpoints");
        endpoints.getTreeState().expandAll();
        endpoints.setOutputMarkupId(true);
        endpoints.setOutputMarkupPlaceholderTag(true);
        add(endpoints);
        
        Form<Object> wiringForm = new Form<Object>("wiringForm");
        initWiringForm(wiringForm);
        add(wiringForm);
        
        ((WiringSubjectTree) globals).setSubject(txtGlobalName);
        ((WiringSubjectTree) endpoints).setSubject(txtInstanceId);
        
        feedbackPanel = new FeedbackPanel("feedbackPanel");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);
    }

    @SuppressWarnings("serial")
    private void initDomainChooseForm(Form<Void> form) {
        domains = new DropDownChoice<Class<? extends Domain>>("domains");
        domains.setOutputMarkupId(true);
        domains.setChoiceRenderer(new ChoiceRenderer<Class<? extends Domain>>("canonicalName"));
        domains.setChoices(createDomainListModel());
        domains.setModel(new Model<Class<? extends Domain>>());
        domains.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Class<? extends Domain> domainType = domains.getModelObject();
                LOGGER.debug("chosen {}", domainType);
                globals.setModel(createGlobalTreeModel(domainType));
                endpoints.setModel(createEndpointsModel(domainType));
                resetWiringForm(target);
                target.addComponent(globals);
                target.addComponent(endpoints);
            }
        });
        form.add(domains);
    }

    @SuppressWarnings("serial")
    private IModel<? extends List<? extends Class<? extends Domain>>> createDomainListModel() {
        return new LoadableDetachableModel<List<? extends Class<? extends Domain>>>() {
                @Override
                protected List<? extends Class<? extends Domain>> load() {
                    List<DomainProvider> serviceList = serviceUtils.listServices(DomainProvider.class);
                    Collections.sort(serviceList, Comparators.forDomainProvider());
                    List<Class<? extends Domain>> domains = new ArrayList<Class<? extends Domain>>();
                    for (DomainProvider dp : serviceList) {
                        domains.add(dp.getDomainInterface());
                    }
                    return domains;
                }
            };
    }
    
    @SuppressWarnings("serial")
    private void initWiringForm(Form<Object> form) {
        form.setOutputMarkupId(true);
        form.setDefaultModel(new CompoundPropertyModel<Object>(this));
        
        txtGlobalName = new TextField<String>("globalName");
        txtGlobalName.setOutputMarkupId(true);
        form.add(txtGlobalName);
        txtInstanceId = new TextField<String>("instanceId");
        txtInstanceId.setOutputMarkupId(true);
        txtInstanceId.setEnabled(false);
        form.add(txtInstanceId);
        
        contextList = new CheckedTree("contextList", createContextModel());
        contextList.getTreeState().expandAll();
        form.add(contextList);
        
        wireButton = new AjaxSubmitLink("wireButton", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                LOGGER.debug("Start wiring {} with {}", globalName, instanceId);
                if (globalName == null || globalName.trim().isEmpty()) {
                    error(new StringResourceModel("globalNotSet", this, null).getString());
                    target.addComponent(feedbackPanel);
                    return;
                }
                if (instanceId == null || instanceId.isEmpty()) {
                    error(new StringResourceModel("instanceIdNotSet", this, null).getString());
                    target.addComponent(feedbackPanel);
                    return;
                }
                if (contextList.getAllChecked().isEmpty()) {
                    error(new StringResourceModel("contextNotSet", this, null).getString());
                    target.addComponent(feedbackPanel);
                    return;
                }
                ConnectorId connectorId = null;
                ConnectorDescription description = null;
                try {
                    connectorId = ConnectorId.fromFullId(instanceId);
                    String domainTypeOfGlobal = getDomainTypeOfGlobal(globalName);
                    String domainTypeOfService = getDomainTypeOfServiceName(connectorId.getDomainType());
                    if (domainTypeOfGlobal != null) {
                        if (alreadySetForOtherDomain(domainTypeOfGlobal, domainTypeOfService)) {
                            info(new StringResourceModel("globalAlreadySet", this, null).getString());
                            target.addComponent(feedbackPanel);
                            LOGGER.info("cannot wire {} with {}, because {} has type {}", 
                                new Object[] {globalName, instanceId, globalName, domainTypeOfGlobal});
                            return;
                        }
                    } else {
                        ruleManager.addGlobal(domainTypeOfService, globalName);
                        LOGGER.info("created global {} of type {}", globalName, domainTypeOfService);
                    }
                    description = serviceManager.getAttributeValues(connectorId);
                } catch (Exception e) {
                    errorWithException(new StringResourceModel("wiringInitError", this, null).getString(), e);
                    resetWiringForm(target);
                    return;
                }
                boolean updated = false;
                ValueMap vmap = new ValueMap();
                vmap.put("globalName", globalName);
                for (String context : contextList.getAllChecked()) {
                    vmap.put("context", context);
                    Model<ValueMap> vmapModel = new Model<ValueMap>(vmap);
                    if (setLocation(globalName, context, description.getProperties())) {
                        updated = true;
                        info(new StringResourceModel("wiringSuccess", this, vmapModel).getString());
                        LOGGER.info("{} got wired with {} in context {}", 
                            new Object[] { globalName, instanceId, context });
                    } else {
                        info(new StringResourceModel("doubleWiring", this, vmapModel).getString());
                        LOGGER.info("{} already wired with {} in context {}", 
                            new Object[] { globalName, instanceId, context });
                    }
                }
                if (updated) {
                    try {
                        serviceManager.forceUpdate(connectorId, description);
                    } catch (Exception e) {
                        errorWithException(new StringResourceModel("wiringError", this, null).getString(), e);
                    } finally {
                        resetWiringForm(target);
                    }
                } else {
                    resetWiringForm(target);
                }
            }
        };
        form.add(wireButton);
    }

    private void errorWithException(String message, Exception e) {
        error(message + "\n" + e.getLocalizedMessage());
        LOGGER.error("Error during wiring", e);
    }

    private String getDomainTypeOfServiceName(String domainName) {
        Filter filter = 
            serviceUtils.makeFilter(DomainProvider.class, String.format("(%s=%s)", Constants.DOMAIN_KEY, domainName));
        DomainProvider dp = (DomainProvider) serviceUtils.getService(filter);
        if (dp == null || dp.getDomainInterface() == null) {
            return null;
        }
        return dp.getDomainInterface().getCanonicalName();
    }

    private String getDomainTypeOfGlobal(String glob) {
        return ruleManager.getGlobalType(glob);
    }

    private boolean alreadySetForOtherDomain(String domainTypeOfGlobal, String domainTypeOfService) {
        return domainTypeOfGlobal != null && !domainTypeOfGlobal.equals(domainTypeOfService);
    }

    /**
     * returns true if location is not already set in the properties, otherwise false 
     */
    private boolean setLocation(String global, String context, Dictionary<String, Object> properties) {
        String locationKey = "location." + context;
        Object propvalue = properties.get(locationKey);
        if (propvalue == null) {
            properties.put(locationKey, global);
        } else if (propvalue.getClass().isArray()) {
            Object[] locations = (Object[]) propvalue;
            if (ArrayUtils.contains(locations, global)) {
                return false;
            }
            Object[] newArray = Arrays.copyOf(locations, locations.length + 1);
            newArray[locations.length] = global;
            properties.put(locationKey, newArray);
        } else {
            if (((String) propvalue).equals(global)) {
                return false;
            }
            Object[] newArray = new Object[2];
            newArray[0] = propvalue;
            newArray[1] = global;
            properties.put(locationKey, newArray);
        }
        return true;
    }

    @SuppressWarnings("serial")
    private IModel<TreeModel> createGlobalTreeModel(final Class<? extends Domain> domainType) {
        return new LoadableDetachableModel<TreeModel>() {
            @Override
            protected TreeModel load() {
                DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Globals");
                if (domainType != null) {
                    for (Entry<String, String> e : ruleManager.listGlobals().entrySet()) {
                        if (e.getValue().equals(domainType.getCanonicalName())) {
                            DefaultMutableTreeNode child = new DefaultMutableTreeNode(e.getKey());
                            rootNode.add(child);
                        }
                    }
                }
                return new DefaultTreeModel(rootNode);
            }
        };
    }
    
    @SuppressWarnings("serial")
    private IModel<TreeModel> createEndpointsModel(final Class<? extends Domain> domainType) {
        return new LoadableDetachableModel<TreeModel>() {
            @Override
            protected TreeModel load() {
                DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Domain endpoints");
                if (domainType != null) {
                    for (Domain d : wiringService.getDomainEndpoints(domainType, "*")) {
                        DefaultMutableTreeNode child = new DefaultMutableTreeNode(d.getInstanceId());
                        rootNode.add(child);
                    }
                }
                return new DefaultTreeModel(rootNode);
            }
        };
    }
    
    private TreeModel createContextModel() {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Contexts");
        for (String c : getAvailableContexts()) {
            rootNode.add(new DefaultMutableTreeNode(c)); 
        }
        return new DefaultTreeModel(rootNode);
    }

    private void resetWiringForm(AjaxRequestTarget target) {
        globalName = "";
        instanceId = "";
        target.addComponent(txtGlobalName);
        target.addComponent(txtInstanceId);
        target.addComponent(feedbackPanel);
    }

    public String getGlobalName() {
        return globalName;
    }

    public void setGlobalName(String globalName) {
        this.globalName = globalName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
    
    @SuppressWarnings("serial")
    private class WiringSubjectTree extends LinkTree {
        private TextField<String> subject;
        
        public WiringSubjectTree(String id) {
            super(id);
             
        }
        
        @Override
        protected void onNodeLinkClicked(Object node, BaseTree tree, AjaxRequestTarget target) {
            DefaultMutableTreeNode mnode = (DefaultMutableTreeNode) node;
            if (mnode.isRoot()) {
                return;
            }
            subject.setDefaultModelObject(mnode.getUserObject());
            target.addComponent(subject);
        }

        @Override
        public boolean isVisible() {
            if (this.getModelObject() == null) {
                return false;
            }
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.getModelObject().getRoot();
            return root != null && !root.isLeaf();
        }

        public void setSubject(TextField<String> subject) {
            this.subject = subject;
        }
    }

    @SuppressWarnings("serial")
    public static class CheckedTree extends LabelTree {
        private Map<String, IModel<Boolean>> checks = new HashMap<String, IModel<Boolean>>();
        
        public CheckedTree(String id, TreeModel model) {
            super(id, model);
        }

        @Override
        protected Component newNodeComponent(String id, IModel<Object> model) {
            DefaultMutableTreeNode mnode = (DefaultMutableTreeNode) model.getObject();
            if (mnode.isRoot()) {
                return super.newNodeComponent(id, model);
            }
            String name = (String) mnode.getUserObject();
            Model<String> labelModel = new Model<String>();
            labelModel.setObject(name);
            Model<Boolean> checkModel = new Model<Boolean>();
            checkModel.setObject(Boolean.FALSE);
            checks.put(name, checkModel);
            return new CheckedPanel(id, checkModel, labelModel);
        }

        @Override
        protected Component newJunctionLink(MarkupContainer parent, final String id, final Object node) {
            return new WebMarkupContainer(id) {
                @Override
                protected void onComponentTag(ComponentTag tag) {
                    super.onComponentTag(tag);
                    tag.setName("span");
                    tag.put("class", "junction-corner");
                }
            };
        }

        public Set<String> getAllChecked() {
            Set<String> checked = new HashSet<String>();
            for (Entry<String, IModel<Boolean>> e : checks.entrySet()) {
                if (Boolean.TRUE.equals(e.getValue().getObject())) {
                    checked.add(e.getKey());
                }
            }
            return checked;
        }
    }
}