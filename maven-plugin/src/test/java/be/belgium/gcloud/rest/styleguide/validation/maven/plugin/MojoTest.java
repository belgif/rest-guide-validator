package be.belgium.gcloud.rest.styleguide.validation.maven.plugin;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class MojoTest extends AbstractMojoTestCase {

    protected void setUp() throws Exception {
        // required for mojo lookups to work
        super.setUp();
    }

    protected void tearDown()throws Exception{
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
        try{
            myMojo.execute();
            fail("Must throw an IllegalArgumentException !");
        }catch (IllegalArgumentException e){
            // all is fine
        }
    }

}
