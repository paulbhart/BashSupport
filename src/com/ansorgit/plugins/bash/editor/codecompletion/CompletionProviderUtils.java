/*
 * Copyright (c) Joachim Ansorg, mail@ansorg-it.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ansorgit.plugins.bash.editor.codecompletion;

import com.ansorgit.plugins.bash.util.OSUtil;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.psi.PsiNamedElement;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

/**
 * @author jansorg
 */
class CompletionProviderUtils {
    private CompletionProviderUtils() {
    }

    static Collection<LookupElement> createPsiItems(Collection<? extends PsiNamedElement> elements) {
        return Collections2.transform(elements, new Function<PsiNamedElement, LookupElement>() {
            public LookupElement apply(PsiNamedElement from) {
                return LookupElementBuilder.create(from).withCaseSensitivity(true);
            }
        });
    }

    static Collection<LookupElement> createItems(Iterable<String> items, final Icon icon) {
        return Lists.transform(Lists.newArrayList(items), new Function<String, LookupElement>() {
            public LookupElement apply(String from) {
                //create a lookup string which doesn't contain typical separator characters
                String lookup = from.replaceAll("[_]", "");
                return LookupElementBuilder.create(from).withCaseSensitivity(true).withLookupString(lookup).withIcon(icon);
            }
        });
    }

    static Collection<LookupElement> wrapInGroup(final int groupId, Collection<LookupElement> elements) {
        return Collections2.transform(elements, new Function<LookupElement, LookupElement>() {
            public LookupElement apply(LookupElement lookupElement) {
                return PrioritizedLookupElement.withGrouping(lookupElement, groupId);
            }
        });
    }

    static Collection<LookupElement> createPathItems(List<String> osPathes) {
        //fix the windows file and directory pathes to be cygwin compatible
        if (SystemInfoRt.isWindows) {
            osPathes = Lists.transform(osPathes, new Function<String, String>() {
                @Override
                public String apply(String path) {
                   return OSUtil.toBashCompatible(path);
                }
            });
        }

        Function<String, LookupElement> transformationFunction = new Function<String, LookupElement>() {
            public LookupElement apply(String path) {
                return new PathLookupElement(path, !path.endsWith("/"));
            }
        };

        Predicate<String> isRelativePath = new Predicate<String>() {
            public boolean apply(String path) {
                return !path.startsWith("/");
            }
        };

        Collection<String> relativePaths = Collections2.filter(osPathes, isRelativePath);
        Collection<LookupElement> relativePathItems = Collections2.transform(relativePaths, transformationFunction);

        Collection<String> absolutePaths = Collections2.filter(osPathes, Predicates.not(isRelativePath));
        Collection<LookupElement> absolutePathItems = Collections2.transform(absolutePaths, transformationFunction);

        Collection<LookupElement> result = Lists.newLinkedList();
        result.addAll(wrapInGroup(CompletionGrouping.RelativeFilePath.ordinal(), relativePathItems));
        result.addAll(wrapInGroup(CompletionGrouping.AbsoluteFilePath.ordinal(), absolutePathItems));

        return result;
    }
}
