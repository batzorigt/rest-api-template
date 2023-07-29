package rest.api;

import java.nio.file.Path;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;

public interface TemplateEngines {

    static TemplateEngine jteEngine() {
        if ("local".equals(API.cfg.environment())) {
            CodeResolver codeResolver = new DirectoryCodeResolver(Path.of("src/main/resources/jte"));
            return TemplateEngine.create(codeResolver, ContentType.Html);
        }

        return TemplateEngine.createPrecompiled(Path.of(API.cfg.jteClassesDir()), ContentType.Html);
    }

}
