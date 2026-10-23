package com.example.saasvilcas.feature.sales.domain;

import java.util.List;

public interface SalesRepository {
    default List<SalesProduct> getCatalog() { return java.util.Collections.emptyList(); }
    default List<SalesSite> loadSites() throws SalesException { return java.util.Collections.emptyList(); }
    default SalesPage loadProducts(SalesFilter filters,int offset) throws SalesException { return new SalesPage(getCatalog(),false,0); }
    default SaleResult submit(SaleDraft draft) throws SalesException { throw new SalesException("El catálogo de prueba no registra ventas.",0,false); }
}
