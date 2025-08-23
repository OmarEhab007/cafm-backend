package com.cafm.cafmbackend.integration.controllers;

import org.junit.platform.suite.api.*;

/**
 * Test suite that runs all controller integration tests.
 * This suite orchestrates testing for all API endpoints.
 */
@Suite
@SuiteDisplayName("CAFM Backend - All Controllers Integration Test Suite")
@SelectPackages("com.cafm.cafmbackend.integration.controllers")
@IncludeTags({"integration", "controller"})
@ExcludeTags("performance") // Performance tests run separately
public class AllControllersTestSuite {
    // This class serves as a test suite runner
    // All test classes in the package will be executed
}