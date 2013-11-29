package org.wso2.carbon.deployment;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.deployment.deployers.CustomDeployer;
import org.wso2.carbon.deployment.exception.CarbonDeploymentException;
import org.wso2.carbon.deployment.spi.Artifact;

import java.io.File;

public class CustomDeployerTest extends BaseTest {
    private final static String DEPLOYER_REPO = "carbon-repo" + File.separator + "text-files";
    private CustomDeployer customDeployer;
    private Artifact artifact;

    /**
     * @param testName
     */
    public CustomDeployerTest(String testName) {
        super(testName);
    }

    @BeforeTest
    public void setup() throws CarbonDeploymentException {
        customDeployer = new CustomDeployer();
        customDeployer.init();
        artifact = new Artifact(new File(getTestResourceFile(DEPLOYER_REPO).getAbsolutePath()
                                               + File.separator + "sample1.txt"));
    }

    @Test
    public void testDeployerInitialization() {
        Assert.assertTrue(CustomDeployer.initCalled);
    }

    @Test(dependsOnMethods = {"testDeployerInitialization"})
    public void testDeploy() throws CarbonDeploymentException {
        customDeployer.deploy(artifact);
        Assert.assertTrue(CustomDeployer.sample1Deployed);
    }

    @Test(dependsOnMethods = {"testDeploy"})
    public void testUnDeploy() throws CarbonDeploymentException {
        customDeployer.undeploy(artifact);
        Assert.assertFalse(CustomDeployer.sample1Deployed);
    }
}
