package build.pluto.buildmonto;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.buildjava.JavaBuilder;
import build.pluto.buildjava.JavaInput;
import build.pluto.buildjava.JavaJar;
import build.pluto.buildjava.JavaJar;
import build.pluto.buildjava.compiler.JavacCompiler;
import build.pluto.buildjava.util.FileExtensionFilter;
import build.pluto.buildmaven.MavenDependencyFetcher;
import build.pluto.buildmaven.input.ArtifactConstraint;
import build.pluto.buildmaven.input.Dependency;
import build.pluto.buildmaven.input.MavenInput;
import build.pluto.buildmonto.util.JavaUtil;
import build.pluto.output.None;
import build.pluto.output.Out;
import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sugarj.common.FileCommands;

public class ServicesBaseJavaBuilder extends Builder<ServicesBaseJavaInput, None> {

    public static BuilderFactory<ServicesBaseJavaInput, None, ServicesBaseJavaBuilder> factory
        = BuilderFactoryFactory.of(ServicesBaseJavaBuilder.class, ServicesBaseJavaInput.class);

    public ServicesBaseJavaBuilder(ServicesBaseJavaInput input) {
        super(input);
    }

    @Override
    public File persistentPath(ServicesBaseJavaInput input) {
        return new File(input.target, "services-base-java.dep");
    }

    @Override
    protected String description(ServicesBaseJavaInput input) {
        return "Build monto:services-base-java";
    }

    @Override
    protected None build(ServicesBaseJavaInput input) throws Throwable {
        //resolve maven dependencies
        MavenInput.Builder mavenInputBuilder = new MavenInput.Builder(
                    new File("lib"),
                    Arrays.asList(MavenDependencies.JEROMQ, MavenDependencies.JSON));
        MavenInput mavenInput = mavenInputBuilder.build();

        //compile services-base-java
        BuildRequest<?, Out<ArrayList<File>>, ?, ?> mavenRequest = new BuildRequest<>(MavenDependencyFetcher.factory, mavenInput);
        Out<ArrayList<File>> mavenOutput = this.requireBuild(mavenRequest);

        // //compile src
        List<BuildRequest<?, ?, ?, ?>> requiredUnits;
        if(input.requiredUnits != null) {
            requiredUnits = new ArrayList<>(input.requiredUnits);
            requiredUnits.add(mavenRequest);
        } else {
            requiredUnits = Arrays.asList(mavenRequest);
        }

        BuildRequest<?, ?, ?, ?> javaRequest = JavaUtil.compileJava(
                input.src,
                input.target,
                mavenOutput.val(),
                requiredUnits);
        requireBuild(javaRequest);

        //build jar out of classfiles

        BuildRequest<?, ?, ?, ?>[] requiredUnitsForJar = { javaRequest };
        BuildRequest<?, ?, ?, ?> jarRequest = JavaUtil.createJar(
                input.target,
                input.jarLocation,
                requiredUnitsForJar);
        this.requireBuild(jarRequest);

        return None.val;
    }
}
