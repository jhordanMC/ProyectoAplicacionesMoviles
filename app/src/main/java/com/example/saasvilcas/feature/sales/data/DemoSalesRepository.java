package com.example.saasvilcas.feature.sales.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.example.saasvilcas.feature.sales.domain.SalesProduct;
import com.example.saasvilcas.feature.sales.domain.SalesRepository;

public final class DemoSalesRepository implements SalesRepository {
    @Override public List<SalesProduct> getCatalog() {
        List<String> sizes=Arrays.asList("38","39","40","41","42");
        List<String> sites=Arrays.asList("Local San Pedro","Local Central");
        return Collections.unmodifiableList(Arrays.asList(
            new SalesProduct("urban","Zapatillas urbanas Adidas",3500,200,"Unisex",sizes,sites),
            new SalesProduct("running","Zapatillas Running Adidas",9500,232,"Hombre",sizes,sites)));
    }
}
