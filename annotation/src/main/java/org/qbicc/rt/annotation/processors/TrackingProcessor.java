package org.qbicc.rt.annotation.processors;

import com.google.auto.service.AutoService;
import org.qbicc.rt.annotation.Tracking;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes({"org.qbicc.rt.annotation.Tracking", "org.qbicc.rt.annotation.Tracking.List"})
@AutoService(Processor.class)
public class TrackingProcessor extends AbstractProcessor {
    private HashSet<String> trackedFiles = new HashSet<>();

    public TrackingProcessor() {
        super();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement te : annotations) {
            for (Element ae : roundEnv.getElementsAnnotatedWith(te)) {
                for (Tracking ta : ae.getAnnotationsByType(org.qbicc.rt.annotation.Tracking.class)) {
                    trackedFiles.add(ta.value());
                }
            }
        }

        if (!trackedFiles.isEmpty()) {
            try {
                FileObject tracked = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", "TrackedFiles.txt");
                PrintWriter pw = new PrintWriter(tracked.openOutputStream(), true);
                trackedFiles.stream()
                        .sorted(Comparator.naturalOrder())
                        .forEach(f -> pw.println(f));
            } catch (IOException e) {
                System.err.println(e);
                e.printStackTrace(System.err);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error writing TrackedFiles.txt");
            }

            trackedFiles.clear();
        }
        return true;
    }

}