package be.belgium.gcloud.rest.styleguide.validation.maven.plugin;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MojoTest extends AbstractMojoTestCase{
    protected void setUp()throws Exception{
        // required
        super.setUp();
    }

    protected void tearDown() throws Exception{
        // required
        super.tearDown();
    }

    public void testApiValidator() throws Exception {
        var pom = getTestFile( "src/test/resources/pom.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        var myMojo = (OpenApiMojo) lookupMojo( "api-validator", pom );
        assertNotNull( myMojo );
        myMojo.execute();
    }

    public void testNoProperties() throws Exception {
        var pom = getTestFile( "src/test/resources/noProperties_pom.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        var myMojo = (OpenApiMojo) lookupMojo( "api-validator", pom );
        assertNotNull( myMojo );
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            myMojo.execute();
        });
    }

    public void testApiValidatorFail() throws Exception {
        var pom = getTestFile( "src/test/resources/pomFail.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        var myMojo = (OpenApiMojo) lookupMojo( "api-validator", pom );
        assertNotNull( myMojo );
        Assertions.assertThrows(MojoFailureException.class, () -> {
            myMojo.execute();
        });
    }
    public void testApiValidatorFailJunit() throws Exception {
        var pom = getTestFile( "src/test/resources/pomFailJunit.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        var myMojo = (OpenApiMojo) lookupMojo( "api-validator", pom );
        assertNotNull( myMojo );
        Assertions.assertThrows(MojoFailureException.class, () -> {
            myMojo.execute();
        });
    }
}
