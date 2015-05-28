package pl.baczkowicz.mqttspy.testcases;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javafx.application.Platform;

import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.baczkowicz.mqttspy.common.generated.ScriptDetails;
import pl.baczkowicz.mqttspy.configuration.ConfigurationManager;
import pl.baczkowicz.mqttspy.scripts.ScriptManager;
import pl.baczkowicz.mqttspy.scripts.ScriptRunningState;
import pl.baczkowicz.mqttspy.ui.TestCaseExecutionController;
import pl.baczkowicz.mqttspy.ui.TestCasesExecutionController;
import pl.baczkowicz.mqttspy.ui.properties.TestCaseProperties;
import pl.baczkowicz.mqttspy.ui.properties.TestCaseStepProperties;
import pl.baczkowicz.mqttspy.utils.FileUtils;

public class TestCaseManager
{	
	final static Logger logger = LoggerFactory.getLogger(TestCaseManager.class);
	
	private final String defaultTestCaseLocation = ConfigurationManager.getDefaultHomeDirectory() + "test_cases";

	private final ScriptManager scriptManager;

	private TestCaseExecutionController testCaseExecutionController;

	private List<TestCaseProperties> testCases = new ArrayList<>();
	
	private List<TestCaseProperties> enqueuedtestCases = new ArrayList<>();

	private TestCasesExecutionController testCasesExecutionController;
	
	private SimpleDateFormat resultFileSdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
	
	private int running = 0;

	public TestCaseManager(final ScriptManager scriptManager, final TestCasesExecutionController testCasesExecutionController, final TestCaseExecutionController testCaseExecutionController)	
	{
		this.scriptManager = scriptManager;
		this.testCaseExecutionController = testCaseExecutionController;
		this.testCasesExecutionController = testCasesExecutionController;
	}
	
	public void loadTestCases(final String testCaseLocation)
	{
		final List<TestCaseProperties> properties = new ArrayList<>();
		
		final List<File> scripts = FileUtils.getDirectoriesWithFile(testCaseLocation, "tc.*js");

		for (final File scriptFile : scripts)
		{
			logger.info("Adding " + scriptFile.getName() + " with parent " + scriptFile.getParent());
			
			final ScriptDetails scriptDetails = new ScriptDetails();					
			scriptDetails.setFile(scriptFile.getAbsolutePath());
			scriptDetails.setRepeat(false);
								
			final String scriptName = ScriptManager.getScriptName(scriptFile);
			
			final TestCase testCase = new TestCase();
					
			scriptManager.createFileBasedScript(testCase, scriptName, scriptFile, scriptManager.getConnection(), scriptDetails);
			
			try
			{	
				scriptManager.runScript(testCase, false);
				testCase.setInfo((TestCaseInfo) scriptManager.invokeFunction(testCase, "getInfo"));
//				testCase.getScriptEngine().eval(new FileReader(testCase.getScriptFile()));
//				final Invocable invocable = (Invocable) testCase.getScriptEngine();
//				
//				testCase.setInfo((TestCaseInfo) invocable.invokeFunction("getInfo"));
				
				int stepNumber = 1;
				for (final String step : testCase.getInfo().getSteps())
				{
					testCase.getSteps().add(new TestCaseStepProperties(
							String.valueOf(stepNumber), step, TestCaseStatus.NOT_RUN, ""));
					stepNumber++;
				}
				
				logger.info(testCase.getInfo().getName() + " " + Arrays.asList(testCase.getInfo().getSteps()));
			}
			catch (ScriptException | NoSuchMethodException e)
			{
				logger.error("Cannot read test case", e);
			}
			
			// Override name
			if (testCase.getInfo() != null && testCase.getInfo().getName() != null)
			{
				testCase.setName(testCase.getInfo().getName());
			}
			else
			{
				testCase.setName(scriptFile.getParentFile().getName());
			}
			
			properties.add(new TestCaseProperties(testCase));
		}
		
		testCases = properties;
		//return properties;
		
		new Thread(new Runnable()
		{			
			@Override
			public void run()
			{
				while (true)
				{
					try
					{
						Thread.sleep(1000);
					}
					catch (InterruptedException e)
					{
						break;
					}
					
					if (enqueuedtestCases.size() > 0 && running == 0)
					{
						runTestCase(enqueuedtestCases.remove(0));
					}
				}
			}
		}).start();
	}
	
	public void runTestCase(final TestCaseProperties selected)
	{			
		final TestCase testCase = selected.getScript();
		
		testCase.setStatus(ScriptRunningState.RUNNING);
		selected.statusProperty().setValue(TestCaseStatus.IN_PROGRESS);
		
		// Clear last run for this test case
		for (final TestCaseStepProperties properties : testCase.getSteps())
		{
			properties.statusProperty().setValue(TestCaseStatus.NOT_RUN);
			properties.executionInfoProperty().setValue("");
		}
		
		new Thread(new Runnable()
		{			
			@Override
			public void run()
			{
				running++;
				TestCaseStepResult lastResult = null;
				testCase.setCurrentStep(0);		
				
				// Run before / setup
				try
				{
					scriptManager.invokeFunction(testCase, "before");
				}
				catch (NoSuchMethodException e)
				{
					logger.info("No setup method present");
				}
				catch (ScriptException e)
				{
					//selected.statusProperty().setValue(TestCaseStatus.FAILED);
					testCase.setStatus(ScriptRunningState.FAILED);					
					logger.error("Step execution failure", e);
				}
				
				while (testCase.getCurrentStep() < testCase.getSteps().size() && testCase.getStatus().equals(ScriptRunningState.RUNNING))
				{
					final TestCaseStepProperties stepProperties = testCase.getSteps().get(testCase.getCurrentStep());
					
					Platform.runLater(new Runnable()
					{							
						@Override
						public void run()
						{
							stepProperties.statusProperty().setValue(TestCaseStatus.IN_PROGRESS);
						}
					});										
					
					try
					{
						// TODO: what to pass: all messages received; messages per topic
						//final IMqttConnection connection = scriptManager.getConnection();
						
						final TestCaseStepResult result = (TestCaseStepResult) scriptManager.invokeFunction(
								testCase, "step" + stepProperties.stepNumberProperty().getValue());
						lastResult = result;
						
						if (result == null)
						{
							// TODO
							continue;
						}
						
						Platform.runLater(new Runnable()
						{							
							@Override
							public void run()
							{
								stepProperties.statusProperty().setValue(result.getStatus());
								stepProperties.executionInfoProperty().setValue(result.getInfo());
							}
						});
						
						// If not in progress any more, move to next
						if (!TestCaseStatus.IN_PROGRESS.equals(result.getStatus()))
						{
							testCase.setCurrentStep(testCase.getCurrentStep() + 1);
						}														
					}
					catch (NoSuchMethodException e)
					{
						stepProperties.statusProperty().setValue(TestCaseStatus.ERROR);
						logger.error("Step execution error", e);
					}
					catch (ScriptException e)
					{
						stepProperties.statusProperty().setValue(TestCaseStatus.FAILED);
						logger.error("Step execution failure", e);
					}
					
					try
					{
						Thread.sleep(1000);
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}		
				}		
				
				// Run after / clean-up
				try
				{
					scriptManager.invokeFunction(testCase, "after");
				}
				catch (NoSuchMethodException e)
				{
					logger.info("No after method present");
				}
				catch (ScriptException e)
				{
					//selected.statusProperty().setValue(TestCaseStatus.FAILED);
					testCase.setStatus(ScriptRunningState.FAILED);					
					logger.error("Step execution failure", e);
				}
				
				final TestCaseStepResult testCaseStatus = lastResult;
				Platform.runLater(new Runnable()
				{							
					@Override
					public void run()
					{
						if (testCase.getStatus().equals(ScriptRunningState.STOPPED))
						{
							selected.statusProperty().setValue(TestCaseStatus.SKIPPED);
						}
						else
						{
							selected.statusProperty().setValue(testCaseStatus.getStatus());
							testCase.setStatus(ScriptRunningState.FINISHED);
						}
						testCaseExecutionController.refreshState();
						testCasesExecutionController.updateContextMenu();
					}
				});
				running--;
				
				if (testCaseExecutionController.isAutoExportEnabled())
				{
					final String parentDir = selected.getScript().getScriptFile().getParent() + System.getProperty("file.separator");
					exportTestCaseResult(selected, new File(parentDir + "result_" + resultFileSdf.format(new Date()) + "_" + testCaseStatus.getStatus()));
				}
			}
		}).start();		
	}

	public void stopTestCase(TestCaseProperties testCaseProperties)
	{
		final TestCase testCase = testCaseProperties.getScript();
		testCase.setStatus(ScriptRunningState.STOPPED);		
		
		final TestCaseStepProperties stepProperties = testCase.getSteps().get(testCase.getCurrentStep());
		
		Platform.runLater(new Runnable()
		{							
			@Override
			public void run()
			{
				stepProperties.statusProperty().setValue(TestCaseStatus.SKIPPED);
			}
		});
	}

	public void enqueueAllTestCases()
	{
		enqueuedtestCases.addAll(testCases);
	}

	public void enqueueTestCase(TestCaseProperties testCaseProperties)
	{
		enqueuedtestCases.add(testCaseProperties);
	}

	public void enqueueAllNotRun()
	{
		for (final TestCaseProperties testCase : getTestCases())
		{
			if (testCase.statusProperty().getValue().equals(TestCaseStatus.NOT_RUN))
			{
				enqueuedtestCases.add(testCase);
			}			
		}
	}

	public void enqueueAllFailed()
	{
		for (final TestCaseProperties testCase : getTestCases())
		{
			if (testCase.statusProperty().getValue().equals(TestCaseStatus.FAILED))
			{
				enqueuedtestCases.add(testCase);
			}			
		}
	}

	public void clearEnqueued()
	{
		enqueuedtestCases.clear();		
	}

	public int getEnqueuedCount()
	{
		return enqueuedtestCases.size();
	}

	public int getTotalCount()
	{
		return testCases.size();
	}

	public List<TestCaseProperties> getTestCases()
	{
		return testCases;
	}

	public void exportTestCaseResult(final TestCaseProperties testCaseProperties, final File selectedFile)
	{
		logger.info("Saving test case results to " + selectedFile.getAbsolutePath());
		
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(selectedFile));
			
			out.write(
					//"Time, " + 
					"Step" + ", " + "\"" + 
					"Description" + "\"" + ", " + 
					"Status" + ", " + "\"" + 
					"Info" + "\"");
			out.newLine();
			
			for (TestCaseStepProperties step : testCaseProperties.getScript().getSteps())
			{
				out.write(
						//step.
						step.stepNumberProperty().getValue() + ", " + "\"" + 
						step.descriptionProperty().getValue() + "\"" + ", " + 
						step.statusProperty().getValue() + ", " + "\"" + 
						step.executionInfoProperty().getValue() + "\"");
				out.newLine();
			}
						
			out.close();
		}
		catch (IOException e)
		{
			logger.error("Cannot write to file", e);
		}
	}
}
