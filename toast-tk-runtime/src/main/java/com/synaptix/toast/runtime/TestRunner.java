package com.synaptix.toast.runtime;

import java.util.Map;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.synaptix.toast.core.event.TestProgressMessage;
import com.synaptix.toast.dao.domain.api.test.ITestResult;
import com.synaptix.toast.dao.domain.impl.test.block.IBlock;
import com.synaptix.toast.dao.domain.impl.test.block.ITestPage;
import com.synaptix.toast.dao.domain.impl.test.block.TestBlock;
import com.synaptix.toast.dao.domain.impl.test.block.line.TestLine;
import com.synaptix.toast.runtime.block.IBlockRunner;

class TestRunner {

	@Inject
	private Map<Class, IBlockRunner> blockRunnerMap;

	@Inject
	private EventBus eventBus;

	/**
	 * Execute the different blocks within the test page
	 * 
	 * @return test page result
	 */
	public ITestPage run(
		final ITestPage testPage,
		final boolean inlineReport
	) {
		testPage.startExecution();
		runTestPageBlocks(testPage, inlineReport);
		enrichTestPageResults(testPage);
		testPage.stopExecution();
		return testPage;
	}

	private ITestPage enrichTestPageResults(ITestPage testPage) {
		int nbSuccess = 0;
		int nbFailures = 0;
		int nbErrors = 0;
		for(final IBlock block : testPage.getBlocks()) {
			if(block instanceof ITestPage) {
				final ITestPage subPage = enrichTestPageResults((ITestPage)block);
				testPage.setTechnicalErrorNumber(testPage.getTechnicalErrorNumber() + subPage.getTechnicalErrorNumber());
				testPage.setTestFailureNumber(testPage.getTestFailureNumber() + subPage.getTestFailureNumber());
				testPage.setTestSuccessNumber(testPage.getTestSuccessNumber() + subPage.getTestSuccessNumber());
				testPage.setIsSuccess(testPage.getTestFailureNumber() + testPage.getTechnicalErrorNumber() == 0);
			}
			else if (block instanceof TestBlock) {
				final TestBlock testBlock = (TestBlock) block;
				int nbBlockSuccess = 0;
				int nbBlockFailures = 0;
				int nbBlockErrors = 0;
				for(final TestLine line : testBlock.getBlockLines()) {
					final ITestResult result = line.getTestResult();
					nbSuccess += result.isSuccess() ? 1 : 0;
					nbBlockSuccess += result.isSuccess() ? 1 : 0;
					nbFailures += result.isFailure() ? 1 : 0;
					nbBlockFailures += result.isFailure() ? 1 : 0;
					nbErrors += result.isError() ? 1 : 0;
					nbBlockErrors += result.isError() ? 1 : 0;
				}
				testBlock.setTechnicalErrorNumber(nbBlockErrors);
				testBlock.setTestSuccessNumber(nbBlockSuccess);
				testBlock.setTestFailureNumber(nbBlockFailures);
			}
		}		
		testPage.setTechnicalErrorNumber(nbErrors);
		testPage.setTestSuccessNumber(nbSuccess);
		testPage.setTestFailureNumber(nbFailures);
		testPage.setIsSuccess(nbFailures + nbErrors == 0);
		return testPage;
	}

	private void runTestPageBlocks(
		final ITestPage testPage, 
		final boolean inlineReport
	) {
		for(final IBlock block : testPage.getBlocks()) {
			if(block instanceof ITestPage) {
				run((ITestPage) block, inlineReport);
				if(inlineReport) {
					eventBus.post(new TestProgressMessage(testPage));
				}
			}
			else {
				final IBlockRunner blockRunner = blockRunnerMap.get(block.getClass());
				if(blockRunner != null){
					blockRunner.run(block);
				}
			}
		}
	}
}