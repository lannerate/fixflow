package com.founder.fix.fixflow.core.impl.persistence.deployer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.di.BpmnDiPackage;
import org.eclipse.bpmn2.util.Bpmn2ResourceFactoryImpl;
import org.eclipse.dd.dc.DcPackage;
import org.eclipse.dd.di.DiPackage;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl.Delegator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import com.founder.fix.bpmn2extensions.fixflow.FixFlowPackage;
import com.founder.fix.fixflow.core.exception.FixFlowException;
import com.founder.fix.fixflow.core.impl.Context;
import com.founder.fix.fixflow.core.impl.bpmn.behavior.DefinitionsBehavior;
import com.founder.fix.fixflow.core.impl.bpmn.behavior.ProcessDefinitionBehavior;
import com.founder.fix.fixflow.core.impl.db.DbSqlSession;
import com.founder.fix.fixflow.core.impl.interceptor.CommandContext;
import com.founder.fix.fixflow.core.impl.persistence.ProcessDefinitionManager;
import com.founder.fix.fixflow.core.impl.persistence.definition.DeploymentEntity;
import com.founder.fix.fixflow.core.impl.persistence.definition.ResourceEntity;
import com.founder.fix.fixflow.core.impl.util.GuidUtil;
import com.founder.fix.fixflow.core.impl.util.ReflectUtil;


/**
 * @author kenshins
 */
public class BpmnDeployer implements Deployer {

	private static final Logger LOG = Logger.getLogger(BpmnDeployer.class.getName());;

	public static final String BPMN_RESOURCE_SUFFIX = "bpmn";
	public static final String[] DIAGRAM_SUFFIXES = new String[] { "png", "jpg", "gif", "svg" };

	

	private ResourceSet getResourceSet() {
		// TODO Auto-generated method stub
		ResourceSet resourceSet= new ResourceSetImpl();
		 ((Delegator) EPackage.Registry.INSTANCE).put("http://www.omg.org/spec/BPMN/20100524/MODEL", Bpmn2Package.eINSTANCE);
			((Delegator) EPackage.Registry.INSTANCE).put("http://www.founderfix.com/fixflow", FixFlowPackage.eINSTANCE);
			((Delegator) EPackage.Registry.INSTANCE).put("http://www.omg.org/spec/DD/20100524/DI", DiPackage.eINSTANCE);
			((Delegator) EPackage.Registry.INSTANCE).put("http://www.omg.org/spec/DD/20100524/DC", DcPackage.eINSTANCE);
			((Delegator) EPackage.Registry.INSTANCE).put("http://www.omg.org/spec/BPMN/20100524/DI", BpmnDiPackage.eINSTANCE);
			FixFlowPackage.eINSTANCE.eClass();
			
			FixFlowPackage xxxPackage = FixFlowPackage.eINSTANCE;
			EPackage.Registry.INSTANCE.put(xxxPackage.getNsURI(), xxxPackage);
			Bpmn2ResourceFactoryImpl ddd = new Bpmn2ResourceFactoryImpl();
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("fixflow", ddd);

			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("bpmn", ddd);
			
			resourceSet.getPackageRegistry().put(xxxPackage.getNsURI(), xxxPackage);
			
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpmn", ddd);
			
			return resourceSet;
	}

	public void deploy(DeploymentEntity deployment) {
		List<ProcessDefinitionBehavior> processDefinitions = new ArrayList<ProcessDefinitionBehavior>();
		Map<String, ResourceEntity> resources = deployment.getResources();

		for (String resourceName : resources.keySet()) {

			LOG.info("资源 " + resourceName +" 发布!");
			if (resourceName.endsWith(BPMN_RESOURCE_SUFFIX)) {
				ResourceEntity resource = resources.get(resourceName);
				byte[] bytes = resource.getBytes();
				ResourceSet resourceSet=getResourceSet();
				
				
				
				String filePath = this.getClass().getClassLoader().getResource("com/founder/fix/fixflow/expand/config/fixflowfile.bpmn")
						.toString();
				Resource ddddResource = null;
				if (!filePath.startsWith("jar")) {
					try {
						filePath = java.net.URLDecoder.decode(ReflectUtil.getResource("com/founder/fix/fixflow/expand/config/fixflowfile.bpmn")
								.getFile(), "utf-8");
					} catch (UnsupportedEncodingException e) {
						// TODO 自动生成的 catch 块
						e.printStackTrace();
						throw new FixFlowException("流程定义文件加载失败！", e);
					}
					ddddResource = resourceSet.createResource(URI.createFileURI(filePath));
				} else {
					ddddResource = resourceSet.createResource(URI.createURI(filePath));
				}
	
			
				try {
					ddddResource.load(new ByteArrayInputStream(bytes), null);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					throw new FixFlowException("定义文件加载失败!", e);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					throw new FixFlowException("定义文件加载失败!", e);
				}
					
					DefinitionsBehavior definitions = (DefinitionsBehavior) ddddResource.getContents().get(0).eContents().get(0);
					
					ProcessDefinitionBehavior process = null;
					for (RootElement rootElement : definitions.getRootElements()) {
						if (rootElement instanceof ProcessDefinitionBehavior) {
							
							//ProcessDefinitionBehavior processObj=(ProcessDefinitionBehavior)rootElement;
							//if(processObj.getProcessDefinitionKey().equals("")){
								process = (ProcessDefinitionBehavior) rootElement;
								break;
							//}
							
							
						}
					}
				
				process.setDefinitions(definitions);

				process.setResourceName(resourceName);
				process.setResourceId(resource.getId());
				processDefinitions.add(process);
				
				//throw new FixFlowException("没有完成！！");
			}

		}

		CommandContext commandContext = Context.getCommandContext();
		ProcessDefinitionManager processDefinitionManager = commandContext.getProcessDefinitionManager();
		//DeploymentCache deploymentCache = Context.getProcessEngineConfiguration().getDeploymentCache();
		DbSqlSession dbSqlSession = commandContext.getDbSqlSession();
		for (ProcessDefinitionBehavior processDefinition : processDefinitions) {
			if (deployment.isNew()) {
				int processDefinitionVersion;

				ProcessDefinitionBehavior latestProcessDefinition = processDefinitionManager.findLatestProcessDefinitionByKey(processDefinition
						.getProcessDefinitionKey());
				if (latestProcessDefinition != null) {
					processDefinitionVersion = latestProcessDefinition.getVersion() + 1;
				} else {
					processDefinitionVersion = 1;
				}

				processDefinition.setVersion(processDefinitionVersion);
				processDefinition.setDeploymentId(deployment.getId());
				
				String processDefinitionId = processDefinition.getProcessDefinitionKey() + ":" + processDefinition.getVersion() + ":"
						+ GuidUtil.CreateGuid(); // GUID

				processDefinition.setProcessDefinitionId(processDefinitionId);

				
				
				
				dbSqlSession.insert("insertProcessDefinition", processDefinition);
				//deploymentCache.addProcessDefinition(processDefinition);

			} else {
				String deploymentId = deployment.getId();
				processDefinition.setDeploymentId(deploymentId);
				ProcessDefinitionBehavior persistedProcessDefinition = processDefinitionManager.findProcessDefinitionByDeploymentAndKey(
						deploymentId, processDefinition.getProcessDefinitionKey());
				processDefinition.setId(persistedProcessDefinition.getId());
				processDefinition.setVersion(persistedProcessDefinition.getVersion());

				//deploymentCache.addProcessDefinition(processDefinition);
			}

			//Context.getProcessEngineConfiguration().getDeploymentCache().addProcessDefinition(processDefinition);
		}
	}

	protected String getDiagramResourceForProcess(String bpmnFileResource, String processKey, Map<String, ResourceEntity> resources) {
		for (String diagramSuffix : DIAGRAM_SUFFIXES) {
			String diagramForBpmnFileResource = getBpmnFileImageResourceName(bpmnFileResource, diagramSuffix);
			String processDiagramResource = getProcessImageResourceName(bpmnFileResource, processKey, diagramSuffix);
			if (resources.containsKey(processDiagramResource)) {
				return processDiagramResource;
			} else if (resources.containsKey(diagramForBpmnFileResource)) {
				return diagramForBpmnFileResource;
			}
		}
		return null;
	}

	protected String getBpmnFileImageResourceName(String bpmnFileResource, String diagramSuffix) {
		String bpmnFileResourceBase = bpmnFileResource.substring(0, bpmnFileResource.length() - 10); // minus
																										// 10
																										// to
																										// remove
																										// 'bpmn20.xml'
		return bpmnFileResourceBase + diagramSuffix;
	}

	protected String getProcessImageResourceName(String bpmnFileResource, String processKey, String diagramSuffix) {
		String bpmnFileResourceBase = bpmnFileResource.substring(0, bpmnFileResource.length() - 10); // minus
																										// 10
																										// to
																										// remove
																										// 'bpmn20.xml'
		return bpmnFileResourceBase + processKey + "." + diagramSuffix;
	}

	protected void createResource(String name, byte[] bytes, DeploymentEntity deploymentEntity) {
		ResourceEntity resource = new ResourceEntity();
		resource.setName(name);
		resource.setBytes(bytes);
		resource.setDeploymentId(deploymentEntity.getId());

		Context.getCommandContext().getDbSqlSession().insert("insertResource", resource);
	}

}
