package com.synaptix.toast.runtime.block;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import com.google.common.eventbus.EventBus;
import com.synaptix.toast.core.annotation.EngineEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.synaptix.toast.adapter.cache.ToastCache;
import com.synaptix.toast.core.report.FailureResult;
import com.synaptix.toast.core.report.SuccessResult;
import com.synaptix.toast.dao.domain.api.test.ITestResult;
import com.synaptix.toast.dao.domain.impl.test.block.TestBlock;
import com.synaptix.toast.dao.domain.impl.test.block.line.TestLine;
import com.synaptix.toast.runtime.ActionItemRepository;
import com.synaptix.toast.runtime.IActionItemRepository;
import com.synaptix.toast.runtime.action.item.ActionItemValueProvider;
import com.synaptix.toast.runtime.bean.ActionCommandDescriptor;
import com.synaptix.toast.runtime.block.locator.ActionAdaptaterLocator;
import com.synaptix.toast.runtime.block.locator.ActionAdaptaterLocators;
import com.synaptix.toast.runtime.block.locator.NoActionAdapterFound;
import com.synaptix.toast.runtime.module.RunnerModule;
import com.synaptix.toast.test.runtime.resource.XmlAdapterExample;

public class BlockRunnerMappingTestCase {

	private static Injector injector;
	TestBlockRunner blockRunner;

	@BeforeClass
	public static void init() {
		ToastCache.getInstance().addActionAdapter(XmlAdapterExample.class);
		Module module = new AbstractModule() {
			@Override
			protected void configure() {
				bind(EventBus.class).annotatedWith(EngineEventBus.class).to(EventBus.class).in(Singleton.class);

				bind(IActionItemRepository.class).to(ActionItemRepository.class).in(Singleton.class);
				bind(ActionItemValueProvider.class).in(Singleton.class);
				bind(XmlAdapterExample.class).in(Singleton.class);
			}
		};
		injector = Guice.createInjector(module, new RunnerModule());
	}
	
	@Before
	public void initRunner(){
		this.blockRunner = injector.getInstance(TestBlockRunner.class);
	}

	@Test
	public void compareTest() {
		String actionSentence = "Comparer *$var2* a *$var1*";
		ActionCommandDescriptor actionDescriptor = blockRunner.findMatchingAction(actionSentence, XmlAdapterExample
				.class);
		Assert.assertNotNull(actionDescriptor);
	}

	@Test
	public void compareTestEmpty() {
		TestBlockRunner blockRunner = new TestBlockRunner();
		String actionSentence = "Comparer ** a *$var1*";
		ActionCommandDescriptor actionDescriptor = blockRunner.findMatchingAction(actionSentence, XmlAdapterExample.class);
		Assert.assertNotNull(actionDescriptor);
	}

	public void executeVoidTest() {
		String actionSentence = "*$var2* == *$var1*";

		TestBlock block = new TestBlock();
		block.setFixtureName("service");
		TestLine line = new TestLine();
		line.setTest(actionSentence);
		block.setBlockLines(Collections.singletonList(line));

		ActionAdaptaterLocator locator;
		try {
			locator = injector.getInstance(ActionAdaptaterLocators.class).getActionCommandDescriptor(block,
					line);

			ITestResult result = blockRunner.invokeActionAdapterAction(locator);
			Assert.assertEquals(SuccessResult.class, result.getClass());
		} catch (NoActionAdapterFound e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void executeStringTest() throws NoActionAdapterFound {
		TestLine line = new TestLine();
		line.setTest("echo *foo*");
		line.setExpected("foo");

		TestBlock block = new TestBlock();
		block.setFixtureName("service");
		block.setBlockLines(Collections.singletonList(line));

		ActionAdaptaterLocator locator =
				injector.getInstance(ActionAdaptaterLocators.class).getActionCommandDescriptor(block, line);
		ITestResult result = blockRunner.invokeActionAdapterAction(locator);

		Assert.assertEquals("foo", result.getMessage());
		Assert.assertEquals(ITestResult.ResultKind.SUCCESS, result.getResultKind());
	}

	@Test
	public void executeStringTestFailing() throws NoActionAdapterFound {
		TestBlock block = new TestBlock();
		block.setFixtureName("service");
		TestLine line = new TestLine();
		line.setTest("echo *foo*");
		line.setExpected("foo");
		block.setBlockLines(Collections.singletonList(line));
		ActionAdaptaterLocators locators = injector.getInstance(ActionAdaptaterLocators.class);
		ActionAdaptaterLocator locator = locators.getActionCommandDescriptor(block, line);
		ITestResult result = blockRunner.invokeActionAdapterAction(locator);

		Assert.assertEquals("foo", locator.getTestLineDescriptor().testLine.getExpected());
		Assert.assertEquals(ITestResult.ResultKind.SUCCESS, result.getResultKind());

		TestLine line2 = new TestLine();
		line2.setTest("echo *foo*");
		line2.setExpected("bar");

		locator = injector.getInstance(ActionAdaptaterLocators.class).getActionCommandDescriptor(block, line2);
		result = blockRunner.invokeActionAdapterAction(locator);

		Assert.assertEquals("bar", locator.getTestLineDescriptor().testLine.getExpected());
		Assert.assertEquals(ITestResult.ResultKind.FAILURE, result.getResultKind());
	}

	@Test
	public void assertTest() throws NoActionAdapterFound {
		String actionSentence = "assert *toto*";

		TestBlock block = new TestBlock();
		block.setFixtureName("service");
		TestLine line = new TestLine();
		line.setTest(actionSentence);
		block.setBlockLines(Collections.singletonList(line));

		ActionAdaptaterLocator locator;

		locator = injector.getInstance(ActionAdaptaterLocators.class).getActionCommandDescriptor(block, line);

		ITestResult result = blockRunner.invokeActionAdapterAction(locator);

		Assert.assertEquals(result.getClass(), SuccessResult.class);
	}
	@Test
	public void assertFailureTest() throws NoActionAdapterFound {
		String actionSentence = "assert not *toto*";

		TestBlock block = new TestBlock();
		block.setFixtureName("service");
		TestLine line = new TestLine();
		line.setTest(actionSentence);
		block.setBlockLines(Collections.singletonList(line));

		ActionAdaptaterLocator locator;
		locator = injector.getInstance(ActionAdaptaterLocators.class).getActionCommandDescriptor(block, line);

		ITestResult result = blockRunner.invokeActionAdapterAction(locator);

		Assert.assertEquals(result.getClass(), FailureResult.class);
	}

	@Test
	public void saveStringTest() throws NoActionAdapterFound {
		String actionSentence = "Say hello";
		ActionCommandDescriptor actionDescriptor = blockRunner.findMatchingAction(actionSentence, XmlAdapterExample.class);

		Assert.assertNotNull(actionDescriptor);
		
		TestBlock block = new TestBlock();
		block.setFixtureName("service");
		TestLine line = new TestLine();
		line.setTest(actionSentence);
		block.setBlockLines(Collections.singletonList(line));

		ActionAdaptaterLocator locator = injector.getInstance(ActionAdaptaterLocators.class).getActionCommandDescriptor(block, line);
	

		ITestResult testResult = blockRunner.invokeActionAdapterAction(locator);
		Assert.assertEquals(ITestResult.ResultKind.SUCCESS, testResult.getResultKind());
		Assert.assertEquals("Hello", testResult.getMessage());
	}

	@Test
	public void compareAndSwapInputsTest() {
		IActionItemRepository repo = injector.getInstance(IActionItemRepository.class);
		Map<String, Object> userVarMap = new HashMap<>();
		userVarMap.put("$var1", "value1");
		userVarMap.put("$var2", "value2");
		repo.setUserVariables(userVarMap);

		String actionSentence = "Comparer *$var2* a *$var1*";
		ToastCache.getInstance().addActionAdapter(XmlAdapterExample.class);
		ActionCommandDescriptor execDescriptor = blockRunner.findMatchingAction(actionSentence, XmlAdapterExample.class);
		System.out.println("BlockRunnerMappingTestCase.compareAndSwapInputsTest() -> " + execDescriptor);
		Object[] args = null;
		try {
			args = blockRunner.buildArgumentList(execDescriptor);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Assert.assertNotNull(args);
		Assert.assertEquals(args.length, 2);
		Assert.assertEquals("value1", args[0]);
		Assert.assertEquals("value2", args[1]);
	}

	@Test
	public void compareAndSwapInputsTest2() {
		IActionItemRepository repo = injector.getInstance(IActionItemRepository.class);
		Map<String, Object> userVarMap = new HashMap<>();
		userVarMap.put("$var1", "value1");
		userVarMap.put("$var2", "value2");
		repo.setUserVariables(userVarMap);

		String actionSentence = "Comparer *$var1* a *$var2*";
		ActionCommandDescriptor method = blockRunner.findMatchingAction(actionSentence, XmlAdapterExample.class);
		Object[] args = null;
		try {
			args = blockRunner.buildArgumentList(method);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Assert.assertNotNull(args);
		Assert.assertEquals(args.length, 2);
		Assert.assertEquals("value2", args[0]);
		Assert.assertEquals("value1", args[1]);
	}

	@Test
	public void testMethodReplacement() {
		String actionSentence = "Inclure le flux xml dans la variable *$flux*";
		ActionCommandDescriptor actionDescriptor = blockRunner.findMatchingAction(actionSentence, XmlAdapterExample.class);
		Assert.assertNotNull(actionDescriptor);
	}

	@Test
	public void testMethodReplacementWithTwoParamaters() {
		String actionSentence = "Inclure dans la variable *$flux* le flux *$xml*";
		ActionCommandDescriptor actionDescriptor = blockRunner.findMatchingAction(actionSentence, XmlAdapterExample.class);
		Assert.assertNotNull(actionDescriptor);
	}

}
